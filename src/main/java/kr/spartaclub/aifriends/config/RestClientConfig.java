package kr.spartaclub.aifriends.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 외부 API 연동을 위한 RestClient 빈 정의.
 *
 * <p>Day 1~2 의 RestClient 학습용 Practice API (JSONPlaceholder, Bored API) 호출용 클라이언트만 남는다.
 * Gemini 호출용 {@code geminiRestClient} 빈은 Day 5 Step 6 에서
 * {@code GeminiService} 와 함께 들어냈다 (Spring AI ChatClient 가 모델 호출을 흡수).</p>
 */
@Configuration
public class RestClientConfig {

    private static final String JSON_PLACEHOLDER_BASE = "https://jsonplaceholder.typicode.com";
    private static final String BORED_API_BASE = "https://bored-api.appbrewery.com";

    @Bean("jsonPlaceholderRestClient")
    public RestClient jsonPlaceholderRestClient() {
        return RestClient.builder()
                .baseUrl(JSON_PLACEHOLDER_BASE)
                .build();
    }

    @Bean("boredRestClient")
    public RestClient boredRestClient() {
        return RestClient.builder()
                .baseUrl(BORED_API_BASE)
                .build();
    }
}
