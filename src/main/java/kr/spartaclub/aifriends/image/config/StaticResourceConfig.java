package kr.spartaclub.aifriends.image.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Day 7 Step 7 — 생성된 이미지를 정적 리소스로 노출.
 *
 * <p>{@code /uploads/**} 요청을 {@code file:./uploads/} 로 매핑한다.
 * 따라서 {@link kr.spartaclub.aifriends.image.service.ImageFileStorageService}
 * 가 돌려준 {@code /uploads/portraits/xxx.jpg} 경로를 그대로 {@code <img src="...">} 에 박아 쓸 수 있다.</p>
 *
 * <p>실제 운영에서는 S3 + CloudFront 같은 CDN 으로 대체하는 것이 정석. 학습용 단순 모양이다.</p>
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final String uploadBaseDir;

    public StaticResourceConfig(
            @Value("${aifriends.image.storage.upload-dir:./uploads/portraits}") String uploadDir) {
        // upload-dir 이 ./uploads/portraits 라면 base 는 ./uploads/ 로 한 단계 위로 매핑한다.
        // (학습용으로는 그냥 고정 ./uploads/ 를 둬도 충분하다.)
        this.uploadBaseDir = "file:./uploads/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadBaseDir);
    }
}
