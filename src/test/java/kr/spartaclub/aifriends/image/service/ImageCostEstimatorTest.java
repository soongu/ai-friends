package kr.spartaclub.aifriends.image.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageCostEstimatorTest {

    private final ImageCostEstimator sut = new ImageCostEstimator();

    @Test
    @DisplayName("Pollinations 모델은 무료(0.0 USD) 로 추정된다")
    void should_return_zero_for_pollinations() {
        assertThat(sut.estimateCostUsd("pollinations-flux")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("OpenAI DALL-E 3 standard 는 약 $0.04 로 추정된다")
    void should_return_dall_e_3_cost() {
        assertThat(sut.estimateCostUsd("openai-dall-e-3-standard")).isEqualTo(0.04);
        assertThat(sut.estimateCostUsd("openai-dall-e-3-hd")).isEqualTo(0.08);
    }

    @Test
    @DisplayName("매핑되지 않은 모델은 0.0 으로 fall-back 한다 (보수적 추정)")
    void should_default_to_zero_for_unknown_model() {
        assertThat(sut.estimateCostUsd("unknown-model")).isEqualTo(0.0);
    }
}
