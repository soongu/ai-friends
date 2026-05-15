package kr.spartaclub.aifriends.hello;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Day 2 과제 2 — compare 프로파일 전용: 두 개의 {@link ChatModel} 을 수동으로 빈 등록한다.
 *
 * <p>Spring AI 의 기본 AutoConfiguration 은 {@code spring.ai.model.chat} 프로퍼티 값에 따라
 * 한 시점에 한 종류의 ChatModel 만 활성화한다. 하지만 /api/compare 에서는 두 프로바이더에
 * 같은 질문을 동시에 던져야 하므로, AutoConfig 를 우회하고 Ollama · OpenAI 호환(Gemini)
 * 구현체를 각각 {@code @Bean("ollamaChatModel")} / {@code @Bean("geminiChatModel")} 으로 등록한다.</p>
 *
 * <p>반환 타입은 구체 구현체가 아닌 {@link ChatModel} 인터페이스로 노출한다 —
 * Day 2 Step 1 원칙("애플리케이션 코드는 ChatModel 인터페이스에만 의존") 을 유지하기 위함.</p>
 */
@Configuration
@Profile("compare")
public class ChatModelCompareConfig {

    @Bean("ollamaChatModel")
    public ChatModel ollamaChatModel(
            @Value("${app.compare.ollama.base-url}") String baseUrl,
            @Value("${app.compare.ollama.model}") String model
    ) {
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();

        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder().model(model).build())
                .build();
    }

    /**
     * compare 프로파일에서 기본 {@link org.springframework.ai.chat.client.ChatClient.Builder}
     * 가 단일 ChatModel 을 찾도록 Gemini 쪽을 {@code @Primary} 로 노출한다.
     * (HelloAiController · BenchmarkController 는 @Qualifier 없이 ChatClient.Builder 만 쓰므로
     *  이 기본값이 있어야 compare 프로파일에서도 기동된다.)
     */
    @Bean("geminiChatModel")
    @Primary
    public ChatModel geminiChatModel(
            @Value("${app.compare.gemini.api-key}") String apiKey,
            @Value("${app.compare.gemini.base-url}") String baseUrl,
            @Value("${app.compare.gemini.completions-path}") String completionsPath,
            @Value("${app.compare.gemini.model}") String model
    ) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .completionsPath(completionsPath)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .build();
    }
}
