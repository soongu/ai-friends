package kr.spartaclub.aifriends.structured.api;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    /**
     * Day 4 Step 3 — {@link BeanOutputConverter} 가 LLM 한테 무엇을 보내는지 들여다보는 디버그 엔드포인트.
     *
     * <p>{@code .entity(Quote.class)} 안쪽에서 Spring AI 가 내부적으로 만들어 쓰는 두 가지 텍스트를
     * 그대로 응답으로 떨어뜨려 학생이 직접 눈으로 확인할 수 있게 한다.</p>
     * <ul>
     *   <li>{@code getJsonSchema()} — record 를 분석해 자동 생성된 순수 JSON Schema 텍스트.</li>
     *   <li>{@code getFormat()} — 위 스키마를 감싸 LLM 한테 "이 형식만 지켜라" 라고 명시하는 자연어 지시문.
     *       {@code .entity()} 호출 시 사용자 프롬프트 끝에 자동 주입되는 것과 동일한 텍스트.</li>
     * </ul>
     *
     * <p>{@code text/plain} 으로 응답하므로 curl 결과를 콘솔에서 그대로 읽기 좋다.</p>
     */
    @GetMapping(value = "/api/structured/quote/format-debug", produces = MediaType.TEXT_PLAIN_VALUE)
    public String quoteFormatDebug() {
        BeanOutputConverter<Quote> converter = new BeanOutputConverter<>(Quote.class);
        return """
                === BeanOutputConverter<Quote>.getJsonSchema() ===
                %s

                === BeanOutputConverter<Quote>.getFormat() ===
                %s
                """.formatted(converter.getJsonSchema(), converter.getFormat());
    }

    /**
     * Day 4 Step 4 — {@code List<Quote>} 반환을 통해 {@link ParameterizedTypeReference} 의 필요성을 보여주는 엔드포인트.
     *
     * <p>{@code .entity(List.class)} 는 컴파일은 통과하지만 원소 타입 정보가 erase 되어
     * 의도한 {@code List<Quote>} 가 아니라 그냥 {@code List<?>} 로 다뤄진다.
     * 그래서 익명 서브클래스 트릭({@code new ParameterizedTypeReference<List<Quote>>() {}})으로
     * 런타임까지 타입 토큰을 살려 보내야 한다.</p>
     */
    @GetMapping("/api/structured/quotes")
    public List<Quote> quotes(
            @RequestParam(defaultValue = "용기") String topic,
            @RequestParam(defaultValue = "3") int count) {
        return chatClient.prompt()
                .user(u -> u.text("'{topic}' 에 관한 짧은 명언을 서로 다른 인물의 것으로 {count} 개 알려줘.")
                        .param("topic", topic)
                        .param("count", count))
                .call()
                .entity(new ParameterizedTypeReference<List<Quote>>() {});
    }

    /**
     * Day 4 Step 4 — {@code Map<String, Integer>} 반환 케이스. 키가 미리 정해지지 않은 동적 응답에 적합한 패턴.
     *
     * <p>record 처럼 컴파일 시점에 키를 고정할 수 없는 데이터(키워드 빈도, 분류 점수 등)를 받을 때
     * Map 으로 받는 게 자연스럽다. {@code List<T>} 와 마찬가지로 {@code ParameterizedTypeReference} 가 필요하다.</p>
     */
    @GetMapping("/api/structured/keyword-counts")
    public Map<String, Integer> keywordCounts(
            @RequestParam(defaultValue = "Spring AI 는 자바 백엔드에서 LLM 을 다루는 표준 추상화를 제공한다.") String text) {
        return chatClient.prompt()
                .user(u -> u.text("다음 문장에서 핵심 명사를 추출해 키워드별 등장 횟수를 JSON 객체로 반환해줘. 문장: {text}")
                        .param("text", text))
                .call()
                .entity(new ParameterizedTypeReference<Map<String, Integer>>() {});
    }
}
