package kr.spartaclub.aifriends.image.config;

import org.springframework.ai.image.ImageModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Day 7 — OpenAI 이미지 모델 직접 등록.
 *
 * <p><b>왜 자동 설정을 쓰지 않는가</b>: 본 프로젝트의 chat 자리는 *Gemini 의 OpenAI 호환 엔드포인트* 를
 * 위해 {@code spring.ai.openai.api-key=${GEMINI_API_KEY}} + {@code base-url=https://generativelanguage...}
 * 로 박혀 있다. Spring AI 의 OpenAI 자동 설정은 *image 자리에도 그 전역 설정* 을 그대로 쓰므로 — image
 * 호출이 *Gemini 호환 엔드포인트* 로 흘러가 401 이 떨어진다. 자동 설정 의존을 끊고 *진짜 OpenAI 키 + URL*
 * 로 빈을 직접 등록한다.</p>
 *
 * <p><b>토글</b>: {@code aifriends.image.provider=openai} 일 때만 활성화. {@code pollinations}
 * (또는 미설정) 면 {@link PollinationsImageModelConfig} 가 그 자리를 차지한다 (§4 프로바이더 추상화).</p>
 *
 * <p><b>application.yml 의 spring.ai.model.image=none</b> 으로 두어 자동 설정이 OpenAiImageModel
 * 빈을 *별도로 만들지 않게* 막는다 (이중 등록 충돌 방지).</p>
 */
@Configuration
@ConditionalOnProperty(name = "aifriends.image.provider", havingValue = "openai")
public class OpenAiImageModelConfig {

    @Bean
    public ImageModel openAiImageModel(
            @Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${spring.ai.openai.image.base-url:https://api.openai.com}") String baseUrl,
            @Value("${spring.ai.openai.image.options.model:dall-e-3}") String model,
            @Value("${spring.ai.openai.image.options.quality:hd}") String quality,
            @Value("${spring.ai.openai.image.options.style:vivid}") String style,
            @Value("${spring.ai.openai.image.options.size:1024x1024}") String size,
            @Value("${spring.ai.openai.image.options.response-format:url}") String responseFormat) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY 가 비어있습니다. .env 에 진짜 OpenAI 키를 박은 뒤 다시 띄워주세요. "
                            + "또는 .env 에 AIFRIENDS_IMAGE_PROVIDER=pollinations 로 토글하면 무료 Pollinations 로 돌아갑니다.");
        }

        OpenAiImageApi api = OpenAiImageApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        OpenAiImageOptions defaults = OpenAiImageOptions.builder()
                .model(model)
                .N(1)
                .responseFormat(responseFormat)
                .build();
        // size / quality / style 은 dall-e-3 전용 필드. dall-e-2 / gpt-image-1 면 무시될 수도 있어 안전하게 setter.
        defaults.setSize(size);
        if (quality != null && !quality.isBlank()) defaults.setQuality(quality);
        if (style != null && !style.isBlank()) defaults.setStyle(style);

        return new OpenAiImageModel(api, defaults, RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }
}
