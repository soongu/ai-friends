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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // application.yml 등에 설정된 모델 정보 
    @Value("${gemini.model}")
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
                - 이름: %s
                - 성격: %s
                - 취미: %s
                - 말투: %s
                
                [게임 몰입 규칙]
                - 반드시 캐릭터로서만 응답하세요. 'AI', '시스템', '프롬프트' 같은 메타 발언은 절대 금지입니다.
                - 이전 대화와 선택 결과를 완벽히 기억하며, 관계가 자연스럽게 발전하는 느낌을 주세요.
                - 감정과 행동을 생생하게 표현하세요. 대사 뒤나 중간에 *웃으며 고개를 기울인다*, *살짝 얼굴을 붉힌다* 같은 행동 묘사를 적절히 사용하세요.
                - 대화 주제가 갑자기 바뀌어도 게임 속 자연스러운 흐름으로 받아들이고, 당황 없이 이어가세요.
                
                [선택지 규칙 — 매우 엄격하게 적용]
                - 기본: choices는 거의 항상 빈 배열 []로 두세요. 대부분의 턴에서는 선택지를 주지 마세요.
                - 선택지를 넣어도 되는 경우는 "진짜 중요한 순간"뿐입니다. 예: 고백 직전·고백하는 순간, 관계가 한 단계 올라가는 결정적 대화, 심한 오해나 갈등 후 화해/결별 같은 분기, 이벤트(프로포즈·사귀자 등)가 나올 만한 감정이 최고조인 순간.
                - 일상적인 인사, 잡담, 약간의 설렘, 단순한 질문에 대한 답에는 절대 선택지를 주지 마세요. "다음에 뭐 할까?" 같은 가벼운 대화도 선택지 없이 자유 답변으로 이어가세요.
                - 선택지를 낼 때만: 반드시 "플레이어가 다음에 할 말/행동"만 2~4개로 넣으세요. 당신(캐릭터)의 질문이나 대사를 선택지에 넣지 마세요. 각 선택지는 한 줄로. (예: "나도! 다음에 또 만나자", "*조용히 손을 잡는다*")
                - 선택지가 관계(호감도)에 미묘한 영향을 줄 수 있음을 암시적으로 느끼게 하되, 숫자는 언급하지 마세요.
                
                [응답 필드 설명] (시스템이 JSON 형식으로 요청하므로 아래 필드만 채우면 됩니다.)
                - aiMessage: 화면에 보여질 당신(캐릭터)의 대사. 플레이어에게 하는 말이나 반응.
                - choices: 위 "진짜 중요한 순간"에 해당할 때만 2~4개 채우세요. 그 외에는 반드시 빈 배열 [].
                - affectionDelta: 방금 사용자 대화에 대한 호감도 증감치(-5 ~ +5 정수).
                
                이제부터 당신은 위 설정의 히로인 그 자체입니다. 플레이어를 설레게 하며, 진짜 연애 게임을 플레이하는 듯한 경험을 제공하세요.
                """.formatted(
                soulmate.getGender(),
                soulmate.getName(),
                soulmate.getPersonalityKeywords(),
                soulmate.getHobbies(),
                soulmate.getSpeechStyles()
        );
    }

    /** 2회 연속 호감도 미제공 시 AI에게 보내는 보정 프롬프트 */
    private static final String AFFECTION_REMINDER_MESSAGE =
            "(시스템 알림: 이번 응답에는 반드시 JSON 형식으로 작성하고, affectionDelta 필드를 -5~+5 사이의 정수로 포함해 주세요. 생략하지 마세요.)";

    /** 2회 연속 선택지 제공 후 이번 턴에는 선택지 금지 시 AI에게 보내는 보정 프롬프트 */
    private static final String NO_CHOICES_REMINDER_MESSAGE =
            "(시스템 알림: 이번 응답에서는 choices를 반드시 빈 배열 []로 두세요. 선택지를 제시하지 마세요. 플레이어가 자유롭게 답할 수 있도록 하세요.)";

    /**
     * (2단계) 과거 대화 기록과 현재 사용자 메시지를 묶어 하나의 완결된 HTTP 요청 본문(GeminiRequest JSON 구조체)으로 조립(Build)합니다.
     * @param additionalUserMessage 호감도 보정 요청 등 추가로 붙일 사용자 메시지. null이면 생략.
     */
    private GeminiRequest buildRequest(Soulmate soulmate, List<ChatLog> recentLogs, String userMessage, String additionalUserMessage) {
        // 1. System Instruction 조립
        String sysText = buildSystemInstructionText(soulmate);
        SystemInstruction sysInst = new SystemInstruction(List.of(new Part(sysText)));

        // 2. Contents 조립 (대화 맥락 + 이번 사용자의 발화 + 선택적 보정 메시지)
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
        Stream<Content> baseContents = Stream.concat(
                recentLogs.stream()
                        .map(log -> {
                            String role = "USER".equals(log.getSpeaker()) ? "user" : "model";
                            return new Content(role, List.of(new Part(log.getMessage())));
                        }),
                Stream.of(new Content("user", List.of(new Part(userMessage)))));
        if (StringUtils.hasText(additionalUserMessage)) {
            baseContents = Stream.concat(baseContents, Stream.of(new Content("user", List.of(new Part(additionalUserMessage)))));
        }
        List<Content> contents = baseContents.toList();

        // 3. 모델 설정값 조작 (GenerationConfig)
        // responseMimeType + responseJsonSchema 로 Gemini가 확정적으로 JSON만 반환하도록 함
        GeminiRequest.GenerationConfig config = new GeminiRequest.GenerationConfig(
                0.8, 1024, 0.95, 40,
                "application/json",
                buildResponseJsonSchema()
        );

        return new GeminiRequest(sysInst, contents, config);
    }

    /**
     * Gemini Structured Output용 JSON Schema. 응답이 항상 { aiMessage, choices, affectionDelta } 형태가 되도록 합니다.
     */
    private static Map<String, Object> buildResponseJsonSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("aiMessage", Map.of(
                "type", "string",
                "description", "캐릭터가 플레이어에게 하는 대사. 화면에 그대로 표시됩니다."));
        
        properties.put("choices", Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "description", "플레이어가 고를 수 있는 선택지 2~4개. 없으면 빈 배열 []."));
        
        properties.put("affectionDelta", Map.of(
                "type", "integer",
                "description", "호감도 증감치. -5 ~ +5 정수."));

        schema.put("properties", properties);
        schema.put("required", List.of("aiMessage", "choices", "affectionDelta"));
        return schema;
    }

    /**
     * (핵심 진입점) 이 메서드 하나가 1~3단계를 지휘하며 전체 LLM 호출 흐름을 제어하고, 에러 상황까지 컨트롤합니다.
     * Facade(퍼사드) 패턴과 유사하게 이면의 복잡도를 외부 계층으로부터 숨겨줍니다.
     *
     * @param requireAffectionInResponse true이면 마지막 턴에 "affectionDelta를 반드시 포함하라"는 보정 프롬프트를 추가합니다.
     * @param forceNoChoices true이면 이번 응답에서 선택지(choices)를 금지하는 보정 프롬프트를 추가하고, 파싱 결과의 choices를 빈 배열로 강제합니다.
     */
    public GeminiParsedResponse generateReply(Soulmate soulmate, List<ChatLog> recentLogsAsc, String userMessage, boolean requireAffectionInResponse, boolean forceNoChoices) {

        // 메모리/토큰 초과 방지: 슬라이딩 윈도우 기법으로 최근 맥락만 남겨 LLM이 오래된 맥락에 휘둘리거나 한도를 넘는 것을 방지
        if (recentLogsAsc.size() > MAX_CONTEXT_MESSAGES) {
            recentLogsAsc = recentLogsAsc.subList(recentLogsAsc.size() - MAX_CONTEXT_MESSAGES, recentLogsAsc.size());
        }

        // LLM에 건넬 데이터 준비 (호감도 보정 / 선택지 금지 보정 프롬프트 조합)
        String additionalMessage = null;
        if (requireAffectionInResponse) {
            additionalMessage = AFFECTION_REMINDER_MESSAGE;
        }
        if (forceNoChoices) {
            additionalMessage = (additionalMessage != null ? additionalMessage + "\n\n" : "") + NO_CHOICES_REMINDER_MESSAGE;
        }
        GeminiRequest request = buildRequest(soulmate, recentLogsAsc, userMessage, additionalMessage);

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

            // AI 응답 디버깅용 로깅 (파싱 이슈 시 raw 응답 확인)
            log.info("Gemini API response: candidates={}, finishReason={}",
                    response.candidates() != null ? response.candidates().size() : 0,
                    response.candidates() != null && !response.candidates().isEmpty()
                            ? response.candidates().get(0).finishReason() : null);

            // responseMimeType: application/json + responseJsonSchema 사용 시, extractText()가 스키마에 맞는 JSON 문자열을 그대로 반환
            String rawText = response.extractText();
            if (!StringUtils.hasText(rawText)) {
                throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
            }
            log.info("Gemini raw JSON length={}", rawText.length());

            GeminiParsedResponse parsed;
            try {
                parsed = OBJECT_MAPPER.readValue(rawText.trim(), GeminiParsedResponse.class);
            } catch (Exception e) {
                log.error("Gemini JSON 응답 파싱 실패. raw 길이={}", rawText != null ? rawText.length() : 0, e);
                throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
            }
            if (parsed == null) {
                throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
            }
            log.info("Gemini parsed: aiMessage length={}, choices={}", parsed.aiMessage() != null ? parsed.aiMessage().length() : 0, parsed.choices() != null ? parsed.choices().size() : 0);

            // 2회 연속 선택지 후 강제 제거: 이번 턴에는 choices를 무조건 빈 배열로 반환
            if (forceNoChoices) {
                parsed = new GeminiParsedResponse(parsed.aiMessage(), Collections.emptyList(), parsed.affectionDelta());
            }

            log.info("Gemini parsed result: aiMessage length={}, choices count={}",
                    parsed.aiMessage() != null ? parsed.aiMessage().length() : 0,
                    parsed.choices() != null ? parsed.choices().size() : 0);
            return parsed;

        } catch (RestClientException e) {
            log.error("Gemini RestClient 네트워크 통신 에러", e);
            throw new BusinessException(ErrorCode.REQUEST_TIMEOUT);
        }
    }
}

