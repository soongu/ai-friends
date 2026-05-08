package kr.spartaclub.aifriends.hello;

import java.util.List;
import java.util.Map;

import kr.spartaclub.aifriends.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 1 ~ Day 4 Spring AI ChatClient 학습용 엔드포인트 모음.
 *
 * <ul>
 *   <li>v1: 시스템 프롬프트 없는 기본 호출 (Day 1 Step 7)</li>
 *   <li>v2: 활성 프로바이더 라벨 + latency 기록 (Day 1 과제)</li>
 *   <li>v3: 구조화 출력 + retry → fallback (Day 4 과제 1) — 시스템 프롬프트는 신규 격리 파일 사용</li>
 *   <li>v3-ab: tutor-v1 / tutor-v2 A/B 분기 + promptVersion 응답 노출 (Day 3 과제 2) — 평문 응답 유지</li>
 * </ul>
 */
@Slf4j
@RestController
public class HelloAiController {

    private final ChatClient chatClient;
    private final ProviderInfo providerInfo;

    /** Day 3 과제 2 — 차분한 튜터 톤(v1) 시스템 프롬프트. v3-ab 의 v1 분기에서 사용. */
    private final PromptTemplate tutorSystemTemplateV1;

    /**
     * Day 3 과제 2 — 쾌활한 튜터 톤(v2) 시스템 프롬프트.
     * v1 과 슬롯은 동일({userName}, {topicTag})이라 분기 시 동일한 Map 을 그대로 재사용 가능.
     */
    private final PromptTemplate tutorSystemTemplateV2;

    /**
     * Day 4 과제 1 — 구조화 출력 전용 시스템 프롬프트 (격리 파일).
     *
     * <p>{@code tutor-v1.st} 의 {@code # Format} 섹션은 *"평문으로 응답한다, JSON 안 됨"* 이라 명시되어 있어
     * v3 를 구조화 응답으로 갈아엎으려면 그 파일을 그대로 수정할 수 없다 (v3-ab 의 평문 가정이 깨진다).
     * 그래서 새 파일 {@code tutor-v3-structured.st} 를 별도로 두고 *"형식은 BeanOutputConverter 에 위임,
     * 의미만 가이드"* 의 톤으로 갈아엎었다.</p>
     */
    private final PromptTemplate tutorSystemTemplateV3Structured;

    /** Day 4 과제 1 — 모든 retry 가 실패했을 때 사용자에게 돌려줄 안전 응답. */
    private static final TutorReply FALLBACK_TUTOR_REPLY = new TutorReply(
            "지금 답을 만들 수 없어요. 잠시 후 다시 질문해주세요.",
            List.of()
    );

    public HelloAiController(
            ChatClient.Builder builder,
            ProviderInfo providerInfo,
            @Value("classpath:prompts/hello/tutor-v1.st") Resource tutorV1Resource,
            @Value("classpath:prompts/hello/tutor-v2.st") Resource tutorV2Resource,
            @Value("classpath:prompts/hello/tutor-v3-structured.st") Resource tutorV3StructuredResource
    ) {
        this.chatClient = builder.build();
        this.providerInfo = providerInfo;
        this.tutorSystemTemplateV1 = new PromptTemplate(tutorV1Resource);
        this.tutorSystemTemplateV2 = new PromptTemplate(tutorV2Resource);
        this.tutorSystemTemplateV3Structured = new PromptTemplate(tutorV3StructuredResource);
    }

    @GetMapping("/api/hello-ai")
    public String hello(
            @RequestParam(defaultValue = "Hello, AI! 한 줄로 자기소개 부탁해.") String message
    ) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/api/hello-ai/v2")
    public HelloResponse helloV2(
            @RequestParam(defaultValue = "Hello, AI! 한 줄로 자기소개 부탁해.") String message
    ) {
        long start = System.currentTimeMillis();

        String reply = chatClient.prompt()
                .user(message)
                .call()
                .content();

        long latencyMs = System.currentTimeMillis() - start;

        return new HelloResponse(
                providerInfo.currentLabel(),
                message,
                reply,
                latencyMs
        );
    }

    /**
     * Day 4 과제 1 — Day 3 의 평문 응답을 {@link TutorReply} record + retry → fallback 으로 업그레이드.
     *
     * <p>핵심 변화 두 가지.</p>
     * <ol>
     *   <li>호출 체인 끝의 {@code .content()} → {@code .entity(TutorReply.class)} — Spring AI 가
     *       record 를 분석해 JSON Schema 를 자동 생성하고 사용자 프롬프트 끝에 format 지시문을 자동 주입한다.
     *       사용자 코드에서 ObjectMapper.readValue try-catch 가 사라진다.</li>
     *   <li>{@code callWithRetryThenFallback} — Step 6 의 {@code recover-retry} 패턴을 그대로 차용해
     *       최대 3 회 재시도 후 모든 시도가 실패하면 안전 fallback record 로 200 OK 를 돌려준다.
     *       시뮬레이션 없이 진짜 LLM 호출이라 모델이 자연스럽게 깨뜨린 경우에만 catch 가 동작한다.</li>
     * </ol>
     */
    @GetMapping("/api/hello-ai/v3")
    public ResponseEntity<ApiResponse<TutorReply>> helloV3(
            @RequestParam(defaultValue = "의존성 주입이 뭔가요?") String message,
            @RequestParam(defaultValue = "Spring AI") String topicTag
    ) {
        String anonymizedUserName = "tutor-student-1";

        String renderedSystemPrompt = tutorSystemTemplateV3Structured.render(Map.of(
                "userName", anonymizedUserName,
                "topicTag", topicTag
        ));

        TutorReply reply = callWithRetryThenFallback(renderedSystemPrompt, message);
        return ResponseEntity.ok(ApiResponse.success(reply));
    }

    /**
     * Day 4 과제 1 — Step 6 의 {@code recover-retry} 패턴을 v3 에 그대로 차용한 helper.
     *
     * <p>최대 3 회 재시도. 모든 시도가 실패하면 {@link #FALLBACK_TUTOR_REPLY} 로 200 OK 응답.
     * 운영 코드에서는 별도 클래스로 빼는 게 맞지만, Day 4 학습 호흡상 컨트롤러 인라인으로 둔다 —
     * Step 6 의 시뮬레이션 패턴과 1:1 비교 가능해 학습 가독성이 더 높다.
     * 본격 운영 정책(써킷브레이커·Rate Limit·캐싱) 과 묶이는 본격 학습은 Day 19 Harness 엔지니어링.</p>
     */
    private TutorReply callWithRetryThenFallback(String systemPrompt, String userMessage) {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                TutorReply reply = chatClient.prompt()
                        .system(systemPrompt)
                        .user(userMessage)
                        .call()
                        .entity(TutorReply.class);
                log.info("hello-ai/v3: attempt {}/{} succeeded", attempt, maxAttempts);
                return reply;
            } catch (RuntimeException e) {
                log.warn("hello-ai/v3: attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());
            }
        }
        log.warn("hello-ai/v3: all {} attempts failed, returning fallback", maxAttempts);
        return FALLBACK_TUTOR_REPLY;
    }

    /**
     * Day 3 과제 2 — userId 해시로 v1/v2 프롬프트 A/B 분기.
     *
     * <p>분기 규칙: userId % 2 == 0 → v1 (차분한 톤), 홀수 → v2 (쾌활·이모지 톤).
     * 로깅/집계는 Day 20 LLM Ops 의 몫이므로 이 단계에서는 응답 DTO 에 promptVersion 을 담아 "보이기" 만 한다.
     * 같은 userId 로 재호출 시 항상 같은 version 으로 분기되므로 sticky assignment 가 보장된다.</p>
     *
     * <p>v3 가 구조화 응답으로 진화한 뒤에도 v3-ab 는 평문 응답({@link HelloAbResponse}) 을 그대로 유지한다 —
     * 이 엔드포인트는 *"같은 슬롯 + 다른 톤 프롬프트 분기"* 학습용이지 구조화 학습 대상이 아니라서다.</p>
     */
    @GetMapping("/api/hello-ai/v3-ab")
    public ResponseEntity<ApiResponse<HelloAbResponse>> helloV3Ab(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "의존성 주입이 뭔가요?") String message,
            @RequestParam(defaultValue = "Spring AI") String topicTag
    ) {
        String anonymizedUserName = "tutor-student-1";

        boolean useV1 = (userId % 2 == 0);
        String promptVersion = useV1 ? "v1" : "v2";
        PromptTemplate chosenTemplate = useV1 ? tutorSystemTemplateV1 : tutorSystemTemplateV2;

        String renderedSystemPrompt = chosenTemplate.render(Map.of(
                "userName", anonymizedUserName,
                "topicTag", topicTag
        ));

        String reply = chatClient.prompt()
                .system(renderedSystemPrompt)
                .user(message)
                .call()
                .content();

        HelloAbResponse body = new HelloAbResponse(promptVersion, anonymizedUserName, topicTag, reply);
        return ResponseEntity.ok(ApiResponse.success(body));
    }
}
