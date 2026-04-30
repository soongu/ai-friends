package kr.spartaclub.aifriends.image.service;

import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Day 7 Step 3 — Pollinations.ai 용 커스텀 {@link ImageModel} 어댑터.
 *
 * <p>Pollinations.ai 는 별도의 인증 키 없이 {@code GET https://image.pollinations.ai/prompt/{prompt}?model=flux&width=1024&height=1024}
 * 형태로 호출하면 그대로 PNG/JPEG 이미지 바이트가 응답으로 내려온다. URL 자체가 결정론적이라
 * 같은 prompt+seed 조합은 같은 이미지를 돌려준다 (캐싱 친화적).</p>
 *
 * <p><b>이 어댑터는 외부 호출을 직접 수행하지 않는다.</b> Pollinations.ai 의 prompt URL 호출 = 이미지 응답이
 * idempotent 하므로, 어댑터는 URL 만 빌드해 {@link Image#getUrl()} 에 담아 돌려주고,
 * 실제 다운로드는 호출자({@link ImageGenerationService}) 가 RestClient 로 별도 처리한다.
 * 이렇게 분리하면 {@link ImageModel} 추상화는 "프롬프트 → 이미지 식별자(URL/b64)" 까지로 좁아지고,
 * 다운로드/저장은 호출자의 책임으로 명확히 갈린다 — 그 결과 OpenAI · Vertex · Stability 등 다른
 * 프로바이더로 갈아끼울 때도 같은 패턴이 그대로 통한다.</p>
 *
 * @see PollinationsImageOptions
 */
public class PollinationsImageModel implements ImageModel {

    private final String baseUrl;

    public PollinationsImageModel(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public ImageResponse call(ImagePrompt request) {
        List<ImageMessage> messages = request.getInstructions();
        String promptText = messages.isEmpty() ? "" : messages.get(0).getText();

        ImageOptions opts = request.getOptions();
        String model = (opts != null && opts.getModel() != null) ? opts.getModel() : "flux";
        Integer width = (opts != null && opts.getWidth() != null) ? opts.getWidth() : 1024;
        Integer height = (opts != null && opts.getHeight() != null) ? opts.getHeight() : 1024;
        Long seed = (opts instanceof PollinationsImageOptions p) ? p.getSeed() : null;

        String encoded = URLEncoder.encode(promptText, StandardCharsets.UTF_8);
        StringBuilder url = new StringBuilder(baseUrl);
        url.append("/prompt/").append(encoded);
        url.append("?model=").append(model);
        url.append("&width=").append(width);
        url.append("&height=").append(height);
        url.append("&nologo=true");
        if (seed != null) {
            url.append("&seed=").append(seed);
        }

        Image image = new Image(url.toString(), null);
        ImageGeneration generation = new ImageGeneration(image);
        return new ImageResponse(List.of(generation));
    }
}
