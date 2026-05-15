package kr.spartaclub.aifriends.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 외부 API 연동을 위한 RestClient 빈 정의.
 *
 * <p>Day 1~2 의 RestClient 학습용 Practice API (JSONPlaceholder, Bored API) 호출용 클라이언트와
 * Day 7 의 이미지 다운로드 전용 클라이언트만 남는다. Gemini 호출용 {@code geminiRestClient} 빈은
 * Day 5 Step 6 에서 {@code GeminiService} 와 함께 들어냈다 (Spring AI ChatClient 가 모델 호출을 흡수).</p>
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

    /**
     * Day 7 Step 4 — 외부 이미지 다운로드 전용 RestClient.
     *
     * <p>이미지 생성·다운로드는 텍스트 호출보다 응답 시간이 길다 (Pollinations 는 cold-start 시
     * 5~20초). 그래서 read-timeout 을 60초로 넉넉히 잡고, 연결 타임아웃은 5초로 짧게 둔다.</p>
     */
    @Bean("externalImageRestClient")
    public RestClient externalImageRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(60000);
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
