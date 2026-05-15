package kr.spartaclub.aifriends.image.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PollinationsImageModelTest {

    private final PollinationsImageModel sut = new PollinationsImageModel("https://image.pollinations.ai");

    @Test
    @DisplayName("프롬프트와 옵션을 받아 Pollinations URL 을 빌드한다")
    void should_build_pollinations_url_with_prompt_and_options() {
        // given
        PollinationsImageOptions options = new PollinationsImageOptions("flux", 512, 768, 42L, null);
        ImagePrompt prompt = new ImagePrompt("a cute pink rabbit", options);

        // when
        ImageResponse response = sut.call(prompt);

        // then
        Image image = response.getResult().getOutput();
        assertThat(image.getUrl())
                .startsWith("https://image.pollinations.ai/prompt/")
                // URLEncoder.encode 는 form-encoding 규칙(공백→'+')을 사용한다.
                // Pollinations.ai 는 둘 다 받아주므로 학습용으로는 충분하다.
                .contains("a+cute+pink+rabbit")
                .contains("model=flux")
                .contains("width=512")
                .contains("height=768")
                .contains("seed=42")
                .contains("nologo=true");
    }

    @Test
    @DisplayName("옵션이 null 이면 model=flux, width/height=1024 기본값을 사용한다")
    void should_use_default_model_and_size_when_options_null() {
        // given
        ImagePrompt prompt = new ImagePrompt("hello world");

        // when
        ImageResponse response = sut.call(prompt);

        // then
        String url = response.getResult().getOutput().getUrl();
        assertThat(url)
                .contains("model=flux")
                .contains("width=1024")
                .contains("height=1024")
                .doesNotContain("seed=");
    }

    @Test
    @DisplayName("한국어 프롬프트는 UTF-8 퍼센트 인코딩으로 안전하게 변환된다")
    void should_url_encode_korean_prompt() {
        // given
        String korean = "귀여운 핑크색 토끼 캐릭터";
        ImagePrompt prompt = new ImagePrompt(korean);

        // when
        ImageResponse response = sut.call(prompt);

        // then
        String expected = URLEncoder.encode(korean, StandardCharsets.UTF_8);
        String url = response.getResult().getOutput().getUrl();
        assertThat(url).contains("/prompt/" + expected);
    }
}
