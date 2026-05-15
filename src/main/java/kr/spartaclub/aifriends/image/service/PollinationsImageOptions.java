package kr.spartaclub.aifriends.image.service;

import org.springframework.ai.image.ImageOptions;

/**
 * Day 7 Step 3 — Pollinations.ai 전용 ImageOptions 구현체.
 *
 * <p>{@link ImageOptions} 표준 인터페이스(model · width · height · style 등)에 더해
 * Pollinations.ai 가 지원하는 {@code seed} 파라미터를 추가로 받기 위해 자체 클래스로 둔다.
 * 표준 옵션만 필요한 경우 {@link org.springframework.ai.image.ImageOptionsBuilder} 로 충분하다.</p>
 *
 * <p>학생 입장에서 의미: <b>프로바이더 고유 옵션은 표준 인터페이스를 확장하는 자체 옵션 클래스</b>
 * 로 흡수한다는 패턴을 보여준다. OpenAI 의 {@code OpenAiImageOptions} 도 동일한 결로
 * {@code quality}, {@code user} 등을 추가 필드로 갖는다.</p>
 */
public class PollinationsImageOptions implements ImageOptions {

    private final String model;
    private final Integer width;
    private final Integer height;
    private final Long seed;
    private final String style;

    public PollinationsImageOptions(String model, Integer width, Integer height, Long seed, String style) {
        this.model = model;
        this.width = width;
        this.height = height;
        this.seed = seed;
        this.style = style;
    }

    public static PollinationsImageOptions defaults() {
        return new PollinationsImageOptions("flux", 1024, 1024, null, null);
    }

    public Long getSeed() {
        return seed;
    }

    @Override
    public Integer getN() {
        return 1;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public Integer getWidth() {
        return width;
    }

    @Override
    public Integer getHeight() {
        return height;
    }

    @Override
    public String getResponseFormat() {
        return "url";
    }

    @Override
    public String getStyle() {
        return style;
    }
}
