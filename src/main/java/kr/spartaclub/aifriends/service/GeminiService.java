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

import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * 구글 Gemini API와의 실제 통신 및 프롬프트 조합, 응답 파싱을 담당하는 외부 인프라스트럭처 서비스입니다.
 * Spring AI 라이브러리 등 추상화 레이어를 쓰지 않고, RestClient를 사용하여 날것(Raw)의 HTTP 요청/응답을 직접 핸들링합니다.
 * 이를 통해 LLM 통신의 밑바닥 동작 원리와 파싱 로직을 명확히 이해할 수 있습니다.
 *
 * <p>Gemini API 사용 가이드 (공식 문서)</p>
 * <ul>
 *   <li>개요 및 시작하기: <a href="https://ai.google.dev/gemini-api/docs">https://ai.google.dev/gemini-api/docs</a></li>
 *   <li>텍스트 생성 (generateContent): <a href="https://ai.google.dev/gemini-api/docs/text-generation">https://ai.google.dev/gemini-api/docs/text-generation</a></li>
 *   <li>REST API 메서드 레퍼런스: <a href="https://ai.google.dev/api">https://ai.google.dev/api</a></li>
 * </ul>
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
                
                [응답 형식 지침 (필수 규칙)]
                당신의 응답은 반드시 아래 JSON 규격을 정확히 준수해야 합니다. 마크다운 코드 블록(```json ... ```)이나 불필요한 텍스트 없이, 순수한 JSON 객체 문자열만 반환하세요.
                
                {
                  "aiMessage": "사용자에게 할 대답 (이성친구로서의 친근한 대화)",
                  "choices": ["영화 보자", "산책하자", "그냥 수다나 떨자"],
                  "affectionDelta": 1
                }
                
                - "aiMessage": 화면에 실제 보여질 당신의 텍스트 답변.
                - "choices": 가끔(예: 대화가 자연스럽게 물어보는 흐름일 때) 사용자에게 제시할 다지선다 옵션 배열(2~4개). 선택지가 필요 없으면 빈 배열 [].
                - "affectionDelta": 방금 사용자의 대화가 당신의 설정된 성격과 취향에 얼마나 잘 맞는지 평가하여 호감도 증감치(-5 ~ +5 정수)를 결정.
                
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
     * 프롬프트를 통해 JSON 형태로 반환하도록 지시했으므로, ObjectMapper를 사용하여 파싱합니다.
     */
    private GeminiParsedResponse parseResponse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new GeminiParsedResponse("응답 생성에 실패했습니다.", Collections.emptyList(), 0);
        }

        try {
            // 외부 모델이 실수로 백틱(```json ... ```)을 붙여서 보내는 경우를 대비하여 텍스트 정제
            String cleanJson = rawText.replaceAll("(?s)^```json\\s*", "")
                                      .replaceAll("(?s)\\s*```$", "")
                                      .trim();
                                      
            tools.jackson.databind.ObjectMapper objectMapper = new tools.jackson.databind.ObjectMapper();
            return objectMapper.readValue(cleanJson, GeminiParsedResponse.class);
            
        } catch (Exception e) {
            log.error("Gemini 응답 JSON 파싱 실패. 원문: {}", rawText, e);
            // 파싱 실패 시 원문을 그대로 aiMessage로 보여주고, 선택지와 호감도 변화는 없도록 Fallback 처리
            return new GeminiParsedResponse(rawText, Collections.emptyList(), 0);
        }
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

