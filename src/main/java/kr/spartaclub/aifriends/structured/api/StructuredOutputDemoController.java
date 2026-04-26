package kr.spartaclub.aifriends.structured.api;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 4 Step 2 — 구조화 출력(Structured Output) PoC 데모 컨트롤러.
 *
 * <p>가장 단순한 record(Quote) 하나로 {@code .call().entity(Class<T>)} 의 동작을
 * 체험하기 위한 일회용 데모이다. SoulmateChatService 같은 본 도메인 코드는
 * Day 4 Step 5 에서 본격적으로 리팩토링한다.</p>
 *
 * <p>Day 2 의 ChatModel 인터페이스 추상화 원칙은 그대로 유지한다.
 * {@link ChatClient.Builder} 는 spring-ai-starter 가 자동 등록하며,
 * 그 뒤의 ChatModel 구현체는 {@code spring.ai.model.chat} 프로퍼티에 의해 결정되므로
 * 사용자 코드는 특정 프로바이더(OpenAI/Ollama/Gemini)에 묶이지 않는다.</p>
 */
@RestController
public class StructuredOutputDemoController {

    private final ChatClient chatClient;

    public StructuredOutputDemoController(ChatClient.Builder builder) {
        // defaultSystem 등 어떤 사전 세팅도 박지 않는다 — Step 2 의 PoC 는
        // "비어있는 ChatClient 에 .entity(Class<T>) 한 줄을 더했을 때
        // 어떤 일이 벌어지는가" 를 가장 정직하게 보여주는 게 목적이다.
        this.chatClient = builder.build();
    }

    /**
     * 구조화 출력 PoC 용 가장 단순한 record.
     *
     * <p>필드 두 개만으로도 BeanOutputConverter 가 자동 JSON Schema 를 생성해
     * 프롬프트에 주입하므로, LLM 이 이 record 형태에 맞는 JSON 을 응답한다.
     * 그 메커니즘은 Day 4 Step 3 에서 직접 들여다본다.</p>
     */
    public record Quote(String text, String author) { }

    /**
     * topic 에 관한 짧은 명언 한 줄을 {@link Quote} record 로 받아 그대로 반환한다.
     *
     * <p>핵심은 마지막 한 줄, {@code .call().entity(Quote.class)} 이다.
     * JSON 스키마 손 조립도, ObjectMapper.readValue try-catch 도 사용자 코드에 등장하지 않는다.</p>
     *
     * @param topic 명언의 주제 (예: "용기", "인내")
     * @return Spring MVC 가 자동으로 JSON 직렬화해서 응답 본문으로 흘려보낸다.
     */
    @GetMapping("/api/structured/quote")
    public Quote quote(@RequestParam(defaultValue = "용기") String topic) {
        return chatClient.prompt()
                .user(u -> u.text("'{topic}' 에 관한 짧은 명언 한 줄을 알려줘.").param("topic", topic))
                .call()
                .entity(Quote.class);
    }
}
