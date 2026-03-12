package kr.spartaclub.aifriends.practice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Practice API(JSONPlaceholder, Bored) 호출용 RestClient 빈 정의.
 * 테스트에서는 MockWebServer URL로 대체 가능.
 */
@Configuration
public class PracticeRestClientConfig {

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
