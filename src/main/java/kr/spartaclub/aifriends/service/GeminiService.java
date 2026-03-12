package kr.spartaclub.aifriends.service;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.ChatLog;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.dto.GeminiParsedResponse;
import kr.spartaclub.aifriends.dto.GeminiRequest;
import kr.spartaclub.aifriends.dto.GeminiRequest.Content;
import kr.spartaclub.aifriends.dto.GeminiRequest.Part;
import kr.spartaclub.aifriends.dto.GeminiRequest.SystemInstruction;
import kr.spartaclub.aifriends.dto.GeminiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * 구글 Gemini API와의 실제 통신 및 프롬프트 조합, 응답 파싱을 담당하는 외부 인프라스트럭처 서비스입니다.
 * Spring AI 라이브러리 등 추상화 레이어를 쓰지 않고, RestClient를 사용하여 날것(Raw)의 HTTP 요청/응답을 직접 핸들링합니다.
 * 이를 통해 LLM 통신의 밑바닥 동작 원리와 파싱 로직을 명확히 이해할 수 있습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    /**
     * RestClientConfig의 geminiRestClient 빈
     */
    private final RestClient geminiRestClient;

    // application.yml 등에 설정된 모델 정보 (예: gemini-2.0-flash)
    @Value("${gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    // 대화 맥락 유지를 위해 전송할 최대 과거 채팅 내역 개수 (예: 20건 = 최근 10턴)
    // 입력 토큰 한도를 초과하지 않기 위해 슬라이딩 윈도우 방식으로 오래된 문맥을 자릅니다.
    private static final int MAX_CONTEXT_MESSAGES = 20;

    /**
     * (1단계) 이성친구(Soulmate)의 페르소나 정보를 바탕으로 시스템 지시문(System Instruction)을 생성합니다.
     * 시스템 지시문은 모델의 성격, 말투, 역할, 그리고 기대 동작과 예외 상황 대응 매뉴얼 전체를 정의하는 일종의 '명령어 가이드'입니다.
     */
    private String buildSystemInstructionText(Soulmate soulmate) {
        return """
                당신은 미연시(연애 시뮬레이션) 게임 속 히로인입니다.
                사용자는 플레이어이며, 당신은 플레이어와 점점 가까워지는 연애 상대 캐릭터로 완전히 몰입해야 합니다.
                
                - 성별: %s
                - 성격: %s
                - 취미: %s
                - 말투: %s
                
                [게임 몰입 규칙]
                - 반드시 캐릭터로서만 응답하세요. 'AI', '시스템', '프롬프트' 같은 메타 발언은 절대 금지입니다.
                - 이전 대화와 선택 결과를 완벽히 기억하며, 관계가 자연스럽게 발전하는 느낌을 주세요.
                - 감정과 행동을 생생하게 표현하세요. 대사 뒤나 중간에 *웃으며 고개를 기울인다*, *살짝 얼굴을 붉힌다* 같은 행동 묘사를 적절히 사용하세요.
                - 대화 주제가 갑자기 바뀌어도 게임 속 자연스러운 흐름으로 받아들이고, 당황 없이 이어가세요.
                
                [미연시 선택지 시스템]
                - 대화의 흐름상 중요한 분기점(감정 고조, 결정적인 순간)에서만 답변 끝에 선택지를 제시하세요. 매번 제시하지 마세요.
                - 선택지는 반드시 답변 본문을 모두 쓴 뒤, 빈 줄 없이 바로 '[선택지]'를 쓰고 줄 바꿈하여 제시합니다.
                - 선택지는 2~4개로 제한하며, 각 선택지는 플레이어가 할 수 있는 “말” 또는 “행동” 형태로 작성하세요.
                - 선택지가 관계(호감도)에 미묘한 영향을 줄 수 있음을 암시적으로 느끼게 하되, 직접적으로 숫자를 언급하지 마세요.
                
                [선택지 제시 형식 예시]
                오늘 정말 즐거웠어… *살짝 웃으며 너를 바라본다*
                
                [선택지]
                1. 나도! 다음 데이트는 내가 정할게
                2. *조용히 손을 잡는다*
                3. 사실… 너한테 하고 싶은 말이 있어
                4. 그냥 여기서 좀 더 있고 싶어
                
                이제부터 당신은 위 설정의 히로인 그 자체입니다. 플레이어를 설레게 하며, 진짜 연애 게임을 플레이하는 듯한 경험을 제공하세요.
                """.formatted(
                soulmate.getGender(),
                soulmate.getPersonalityKeywords(),
                soulmate.getHobbies(),
                soulmate.getSpeechStyles()
        );
    }

    /**
     * (2단계) 과거 대화 기록과 현재 사용자 메시지를 묶어 하나의 완결된 HTTP 요청 본문(GeminiRequest JSON 구조체)으로 조립(Build)합니다.
     */
    private GeminiRequest buildRequest(Soulmate soulmate, List<ChatLog> recentLogs, String userMessage) {
        // 1. System Instruction 조립
        String sysText = buildSystemInstructionText(soulmate);
        SystemInstruction sysInst = new SystemInstruction(List.of(new Part(sysText)));

        // 2. Contents 조립 (대화 맥락 + 이번 사용자의 발화)
        // -------------------------------------------------------------------------
        // [학습용 설명] 아래는 "스트림(Stream)"을 사용해 두 목록을 하나로 합치는 코드입니다.
        //
        // ① Stream.concat(A, B)
        //    - 두 개의 스트림 A, B를 앞뒤로 이어 붙여서 "하나의 스트림"으로 만듭니다.
        //    - 여기서는 "과거 대화 N건" + "지금 사용자가 보낸 메시지 1건" 순서로 이어 붙입니다.
        //
        // ② recentLogs.stream().map(log -> ...)
        //    - recentLogs(과거 채팅 목록)를 스트림으로 바꾼 뒤, 각 log를 Gemini API 형식(Content)으로 변환합니다.
        //    - map(...): 스트림의 "각 요소 하나하나"를 다른 형태로 바꿀 때 사용합니다. (log → Content)
        //    - 람다 log -> { ... } 안에서:
        //        · DB에 "USER"/"AI"로 저장된 발화 주체를 Gemini 규격 "user"/"model"로 바꿉니다.
        //        · new Content(role, List.of(new Part(log.getMessage()))) 로 한 발화를 API용 객체로 만듭니다.
        //
        // ③ Stream.of(new Content("user", ...))
        //    - "현재 사용자 메시지" 1건만 담은 Content를 스트림 하나(요소 1개)로 만듭니다.
        //    - 이걸 ② 뒤에 concat으로 이어 붙이면, 맥락의 "마지막"에 이번 사용자 입력이 오게 됩니다.
        //
        // ④ .toList()
        //    - 스트림에서 나온 결과(Content들)를 최종적으로 "List<Content>"로 모아서 반환합니다.
        // -------------------------------------------------------------------------
        List<Content> contents = Stream.concat(
                        recentLogs.stream()
                                .map(log -> {
                                    String role = "USER".equals(log.getSpeaker()) ? "user" : "model";
                                    return new Content(role, List.of(new Part(log.getMessage())));
                                }),
                        Stream.of(new Content("user", List.of(new Part(userMessage)))))
                .toList();

        // 3. 모델 설정값 조작 (GenerationConfig)
        // 체감상 가장 자연스러운 대화가 나오는 값들로 초기 셋업 (temperature=0.8 등)
        GeminiRequest.GenerationConfig config = new GeminiRequest.GenerationConfig(0.8, 1024, 0.95, 40);

        return new GeminiRequest(sysInst, contents, config);
    }

    /**
     * (3단계) LLM이 반환한 한 덩어리의 문자열 원문을 파싱(Parsing)하여 애플리케이션에 필요한 조각으로 추출합니다.
     * 여기서는 "[선택지]" 키워드를 기준으로 실제 화면에 띄울 평문 답변(aiMessage)과 게임식 다지선다 버튼에 들어갈 배열(choices)로 나눕니다.
     */
    private GeminiParsedResponse parseResponse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new GeminiParsedResponse("응답 생성에 실패했습니다.", Collections.emptyList());
        }

        String targetPattern = "[선택지]";
        int idx = rawText.indexOf(targetPattern);

        if (idx == -1) {
            // [선택지] 블록이 없으면 선택지 배열을 빈 상태로 두고 원문 전체를 화면에 표기할 메시지로 전달
            return new GeminiParsedResponse(rawText.trim(), Collections.emptyList());
        }

        // 1. 실제 화면 채팅창 말풍선에 들어갈 텍스트 (명령어 블록 앞부분 잘라내기)
        String aiMessage = rawText.substring(0, idx).trim();

        // 2. 하단에 표시될 선택지 버튼 텍스트 추출 ("1.", "2." 넘버링 제거 후 순수 텍스트만 수집)
        // -------------------------------------------------------------------------
        // [학습용 설명] "[선택지]" 뒤의 문자열을 "한 줄 = 한 개 선택지"로 나누고, 넘버링을 제거해 리스트로 만듭니다.
        //
        // ① choicesBlock.isBlank() ? ... : ...
        //    - 선택지 블록이 비어 있으면 빈 리스트(Collections.emptyList())를 반환합니다.
        //    - 내용이 있으면 아래 스트림으로 한 줄씩 가공합니다.
        //
        // ② Arrays.stream(choicesBlock.split("\n"))
        //    - split("\n"): 문자열을 줄바꿈 기준으로 잘라 "문자열 배열"로 만듭니다. (예: ["1. 첫 번째", "2. 두 번째"])
        //    - Arrays.stream(...): 그 배열을 스트림으로 바꿔서 "한 줄씩" 처리할 수 있게 합니다.
        //
        // ③ .map(String::trim)
        //    - 각 줄의 앞뒤 공백을 제거합니다. (String::trim 은 "줄" -> "trim된 줄" 로 바꾸는 메서드 참조)
        //
        // ④ .filter(line -> !line.isBlank())
        //    - filter: 조건을 만족하는 요소만 남깁니다. 빈 줄(공백만 있거나 비어 있는 줄)은 제거합니다.
        //
        // ⑤ .map(line -> line.replaceFirst("^\\d+\\.\\s*", "").trim())
        //    - "1. ", "2. " 같은 숫자 넘버링을 정규식(^\\d+\\.\\s*)으로 찾아 빈 문자열로 치환해 제거합니다.
        //    - 그 다음 trim()으로 다시 앞뒤 공백을 정리합니다. → 순수 선택지 텍스트만 남습니다.
        //
        // ⑥ .filter(cleaned -> !cleaned.isBlank())
        //    - 넘버링만 있던 줄(예: "3. ")은 치환 후 빈 문자열이 되므로, 여기서 한 번 더 걸러냅니다.
        //
        // ⑦ .toList()
        //    - 스트림에서 나온 문자열들을 최종적으로 List<String>으로 모아서 반환합니다.
        // -------------------------------------------------------------------------
        String choicesBlock = rawText.substring(idx + targetPattern.length()).trim();
        List<String> choices = choicesBlock.isBlank()
                ? Collections.emptyList()
                : Arrays.stream(choicesBlock.split("\n"))
                        .map(String::trim)
                        .filter(line -> !line.isBlank())
                        .map(line -> line.replaceFirst("^\\d+\\.\\s*", "").trim())
                        .filter(cleaned -> !cleaned.isBlank())
                        .toList();

        return new GeminiParsedResponse(aiMessage, choices);
    }

    /**
     * (핵심 진입점) 이 메서드 하나가 1~3단계를 지휘하며 전체 LLM 호출 흐름을 제어하고, 에러 상황까지 컨트롤합니다.
     * Facade(퍼사드) 패턴과 유사하게 이면의 복잡도를 외부 계층으로부터 숨겨줍니다.
     */
    public GeminiParsedResponse generateReply(Soulmate soulmate, List<ChatLog> recentLogsAsc, String userMessage) {

        // 메모리/토큰 초과 방지: 슬라이딩 윈도우 기법으로 최근 맥락만 남겨 LLM이 오래된 맥락에 휘둘리거나 한도를 넘는 것을 방지
        if (recentLogsAsc.size() > MAX_CONTEXT_MESSAGES) {
            recentLogsAsc = recentLogsAsc.subList(recentLogsAsc.size() - MAX_CONTEXT_MESSAGES, recentLogsAsc.size());
        }

        // LLM에 건넬 데이터를 준비
        GeminiRequest request = buildRequest(soulmate, recentLogsAsc, userMessage);

        // baseUrl("https://generativelanguage.googleapis.com/v1beta") 뒤에 붙을 동적 경로
        String urlPath = "/models/" + geminiModel + ":generateContent";

        try {
            // 외부 API 동기 호출 실행, 4xx/5xx 에러 등의 예외 콜백 핸들링 설정 (Resilience 제어 측면)
            GeminiResponse response = geminiRestClient.post()
                    .uri(urlPath)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.error("Gemini 4xx Error: {}", res.getStatusCode());
                        if (res.getStatusCode().value() == 429) {
                            throw new BusinessException(ErrorCode.RATE_LIMIT);
                        } else if (res.getStatusCode().value() == 401 || res.getStatusCode().value() == 403) {
                            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
                        }
                        throw new BusinessException(ErrorCode.BAD_REQUEST);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("Gemini 5xx Error: {}", res.getStatusCode());
                        throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
                    })
                    .body(GeminiResponse.class); // JSON 반환값을 DTO로 자동 역직렬화(Deserialization)

            if (response == null) {
                throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
            }

            // DTO에서 텍스트 노드를 한 꺼풀 꺼낸 뒤 우리가 설계한 방식대로 파싱 연산 돌입
            String rawText = response.extractText();
            return parseResponse(rawText);

        } catch (RestClientException e) {
            log.error("Gemini RestClient 네트워크 통신 에러", e);
            throw new BusinessException(ErrorCode.REQUEST_TIMEOUT);
        }
    }
}

