package kr.spartaclub.aifriends.image.config;

import kr.spartaclub.aifriends.image.service.PollinationsImageModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Day 7 Step 3 — Pollinations.ai 어댑터를 {@link ImageModel} 인터페이스 빈으로 등록.
 *
 * <p>리턴 타입은 일부러 구현체 {@code PollinationsImageModel} 이 아니라
 * 인터페이스 {@link ImageModel} 로 선언한다. 서비스 계층은 인터페이스에만 의존하므로,
 * Pollinations 대신 {@code OpenAiImageModel} 이나 {@code StabilityAiImageModel} 로
 * 갈아끼우려면 이 빈만 교체하면 된다 (§4 프로바이더 추상화 게이트).</p>
 *
 * <p><b>토글</b>: {@code aifriends.image.provider} 가 {@code pollinations} (또는 미설정) 일 때만
 * 이 빈이 활성화된다. {@code openai} 로 두면 — Pollinations 빈은 비활성, Spring AI 의
 * {@code spring-ai-starter-model-openai} 가 자동으로 {@code OpenAiImageModel} 빈을 등록한다.</p>
 */
@Configuration
@ConditionalOnProperty(
        name = "aifriends.image.provider",
        havingValue = "pollinations",
        matchIfMissing = true)
public class PollinationsImageModelConfig {

    @Bean
    public ImageModel pollinationsImageModel(
            @Value("${aifriends.image.pollinations.base-url:https://image.pollinations.ai}") String baseUrl) {
        return new PollinationsImageModel(baseUrl);
    }
}
