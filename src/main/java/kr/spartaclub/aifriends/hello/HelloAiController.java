package kr.spartaclub.aifriends.hello;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
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
 * </ul>
 */
@RestController
public class HelloAiController {

    private final ChatClient chatClient;
    private final ProviderInfo providerInfo;

    /**
     * Day 3 과제 1 — 친근한 동료 튜터 페르소나 시스템 프롬프트.
     *
     * <p>본문은 src/main/resources/prompts/hello/tutor-v1.st 에 있다.
     * 부팅 시 한 번만 파싱되어 PromptTemplate 인스턴스로 보관되고,
     * render(Map) 은 매 호출마다 새로운 Map 을 받아 문자열을 만들어 반환하므로 스레드 안전하다.</p>
     */
    private final PromptTemplate tutorSystemTemplate;

    public HelloAiController(
            ChatClient.Builder builder,
            ProviderInfo providerInfo,
            @Value("classpath:prompts/hello/tutor-v1.st") Resource tutorSystemResource
    ) {
        this.chatClient = builder.build();
        this.providerInfo = providerInfo;
        this.tutorSystemTemplate = new PromptTemplate(tutorSystemResource);
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
     */
    @GetMapping("/api/hello-ai/v3")
    public String helloV3(
            @RequestParam(defaultValue = "의존성 주입이 뭔가요?") String message,
            @RequestParam(defaultValue = "Spring AI") String topicTag
    ) {
        String anonymizedUserName = "tutor-student-1";

        String renderedSystemPrompt = tutorSystemTemplate.render(Map.of(
                "userName", anonymizedUserName,
                "topicTag", topicTag
        ));

        return chatClient.prompt()
                .system(renderedSystemPrompt)
                .user(message)
                .call()
                .content();
    }
}
