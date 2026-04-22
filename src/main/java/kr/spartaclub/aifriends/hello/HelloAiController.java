package kr.spartaclub.aifriends.hello;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 1 Step 7 — Spring AI ChatClient 첫 호출 엔드포인트.
 *
 * <p>활성 프로파일(ollama/gemini)에 따라 내부 ChatModel 이 바뀌지만
 * 이 컨트롤러 코드는 단 한 줄도 바뀌지 않는다. 프로바이더 추상화의 실증.</p>
 *
 * <p>Day 1 과제 1 — /api/hello-ai/v2 는 응답에 활성 프로바이더 라벨과 latency 를
 * 함께 담아 같은 질문을 두 프로파일에서 호출했을 때 비교가 쉬워지게 한다.</p>
 */
@RestController
public class HelloAiController {

    private final ChatClient chatClient;
    private final ProviderInfo providerInfo;

    public HelloAiController(ChatClient.Builder builder, ProviderInfo providerInfo) {
        this.chatClient = builder.build();
        this.providerInfo = providerInfo;
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
}
