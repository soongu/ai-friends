package kr.spartaclub.aifriends.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 외부 API 연동을 위한 RestClient 빈 정의.
 * Gemini API 및 Practice API(JSONPlaceholder, Bored API) 호출용 클라이언트를 제공합니다.
 */
@Configuration
public class RestClientConfig {

    private static final String JSON_PLACEHOLDER_BASE = "https://jsonplaceholder.typicode.com";
    private static final String BORED_API_BASE = "https://bored-api.appbrewery.com";

    @Value("${gemini.base-url}")
    private String geminiBaseUrl;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

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

    @Bean("geminiRestClient")
    public RestClient geminiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5초 안에 연결 안 되면 포기 (이걸 안해놓으면 구글서버가 뻗어있으면 우리도 무한으로 대기하다 뻗음)
        factory.setReadTimeout(30000);   // 연결 후 30초 안에 응답 안 오면 포기

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(geminiBaseUrl)
                .defaultHeader("x-goog-api-key", geminiApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
