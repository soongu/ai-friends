package kr.spartaclub.aifriends.hello;

import java.util.Map;

import kr.spartaclub.aifriends.common.response.ApiResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 1 ~ Day 3 Spring AI ChatClient 학습용 엔드포인트 모음.
 *
 * <ul>
 *   <li>v1: 시스템 프롬프트 없는 기본 호출 (Day 1 Step 7)</li>
 *   <li>v2: 활성 프로바이더 라벨 + latency 기록 (Day 1 과제)</li>
 *   <li>v3: 외부 파일 시스템 프롬프트 + {userName}·{topicTag} 바인딩 (Day 3 과제 1)</li>
 *   <li>v3-ab: tutor-v1 / tutor-v2 A/B 분기 + promptVersion 응답 노출 (Day 3 과제 2)</li>
 * </ul>
 */
@RestController
public class HelloAiController {

    private final ChatClient chatClient;
    private final ProviderInfo providerInfo;

    /** Day 3 과제 1 — 차분한 튜터 톤(v1) 시스템 프롬프트. 과제 2 이후 V1/V2 쌍의 기본축으로 이름을 명시화. */
    private final PromptTemplate tutorSystemTemplateV1;

    /**
     * Day 3 과제 2 — 쾌활한 튜터 톤(v2) 시스템 프롬프트.
     * v1 과 슬롯은 동일({userName}, {topicTag})이라 분기 시 동일한 Map 을 그대로 재사용 가능.
     */
    private final PromptTemplate tutorSystemTemplateV2;

    public HelloAiController(
            ChatClient.Builder builder,
            ProviderInfo providerInfo,
            @Value("classpath:prompts/hello/tutor-v1.st") Resource tutorV1Resource,
            @Value("classpath:prompts/hello/tutor-v2.st") Resource tutorV2Resource
    ) {
        this.chatClient = builder.build();
        this.providerInfo = providerInfo;
        this.tutorSystemTemplateV1 = new PromptTemplate(tutorV1Resource);
        this.tutorSystemTemplateV2 = new PromptTemplate(tutorV2Resource);
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
     * Day 3 과제 1 — 외부 파일 시스템 프롬프트 + {userName}·{topicTag} 바인딩.
     *
     * <p>유저 질문 자체는 매번 바뀌지만 튜터 페르소나·답변 규칙·예시는 고정이라
     * 시스템 프롬프트 영역에 몰아넣었다. 익명 ID 는 실서비스에서는 세션/유저 엔티티의
     * 익명화 컬럼에서 읽어오지만, 과제 범위에서는 고정 문자열로 충분하다.</p>
     *
     * <p>응답은 {@link TutorReply} record 를 {@code ApiResponse<T>} 로 감싸 CLAUDE.md 4-1 게이트와 정합.</p>
     */
    @GetMapping("/api/hello-ai/v3")
    public ResponseEntity<ApiResponse<TutorReply>> helloV3(
            @RequestParam(defaultValue = "의존성 주입이 뭔가요?") String message,
            @RequestParam(defaultValue = "Spring AI") String topicTag
    ) {
        String anonymizedUserName = "tutor-student-1";

        String renderedSystemPrompt = tutorSystemTemplateV1.render(Map.of(
                "userName", anonymizedUserName,
                "topicTag", topicTag
        ));

        String reply = chatClient.prompt()
                .system(renderedSystemPrompt)
                .user(message)
                .call()
                .content();

        return ResponseEntity.ok(ApiResponse.success(new TutorReply(topicTag, reply)));
    }

    /**
     * Day 3 과제 2 — userId 해시로 v1/v2 프롬프트 A/B 분기.
     *
     * <p>분기 규칙: userId % 2 == 0 → v1 (차분한 톤), 홀수 → v2 (쾌활·이모지 톤).
     * 로깅/집계는 Day 20 LLM Ops 의 몫이므로 이 단계에서는 응답 DTO 에 promptVersion 을 담아 "보이기" 만 한다.
     * 같은 userId 로 재호출 시 항상 같은 version 으로 분기되므로 sticky assignment 가 보장된다.</p>
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
