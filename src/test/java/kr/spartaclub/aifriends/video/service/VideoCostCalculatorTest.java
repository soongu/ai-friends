package kr.spartaclub.aifriends.video.service;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.video.dto.VideoGenerationRequest;
import kr.spartaclub.aifriends.video.dto.VideoModelTier;
import kr.spartaclub.aifriends.video.exception.VideoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Day 10 — 비디오 비용 계산기 검증.
 *
 * <p>가격 = 모델 티어 단가 × 길이(초) × 해상도 가산. *비용 감각 배양용 추정치* 라
 * 청구서와 정확히 같지 않다는 결을 잊지 말 것.</p>
 */
class VideoCostCalculatorTest {

    private final VideoCostCalculator calculator = new VideoCostCalculator();

    @Test
    @DisplayName("Sora 5초 720p — 1.00 × 5 × 2 = $10.00")
    void should_calculate_sora_5s_720p() {
        VideoGenerationRequest request = new VideoGenerationRequest("epic shot", 5, "720p");

        double cost = calculator.estimateCostUsd(request, VideoModelTier.SORA);

        assertThat(cost).isCloseTo(10.00, within(0.001));
    }

    @Test
    @DisplayName("Veo 3 5초 1080p — 0.50 × 5 × 4 = $10.00")
    void should_calculate_veo3_5s_1080p() {
        VideoGenerationRequest request = new VideoGenerationRequest("seaside cinematic", 5, "1080p");

        double cost = calculator.estimateCostUsd(request, VideoModelTier.VEO_3);

        assertThat(cost).isCloseTo(10.00, within(0.001));
    }

    @Test
    @DisplayName("Kling 5초 480p — 0.06 × 5 × 1 = $0.30")
    void should_calculate_kling_5s_480p() {
        VideoGenerationRequest request = new VideoGenerationRequest("idle scene", 5, "480p");

        double cost = calculator.estimateCostUsd(request, VideoModelTier.KLING);

        assertThat(cost).isCloseTo(0.30, within(0.001));
    }

    @Test
    @DisplayName("로컬 SVD 는 항상 $0.00")
    void should_calculate_local_svd_zero() {
        VideoGenerationRequest request = new VideoGenerationRequest("anything", 5, "720p");

        double cost = calculator.estimateCostUsd(request, VideoModelTier.STABLE_VIDEO_DIFFUSION_LOCAL);

        assertThat(cost).isCloseTo(0.00, within(0.001));
    }

    @Test
    @DisplayName("길이가 0 초면 VIDEO_DURATION_INVALID")
    void should_throw_when_duration_zero() {
        VideoGenerationRequest request = new VideoGenerationRequest("p", 0, "720p");

        assertThatThrownBy(() -> calculator.estimateCostUsd(request, VideoModelTier.SORA))
                .isInstanceOf(VideoException.class)
                .hasMessageContaining(ErrorCode.VIDEO_DURATION_INVALID.getMessage());
    }

    @Test
    @DisplayName("길이가 11 초면 VIDEO_DURATION_INVALID")
    void should_throw_when_duration_too_long() {
        VideoGenerationRequest request = new VideoGenerationRequest("p", 11, "720p");

        assertThatThrownBy(() -> calculator.estimateCostUsd(request, VideoModelTier.SORA))
                .isInstanceOf(VideoException.class)
                .hasMessageContaining(ErrorCode.VIDEO_DURATION_INVALID.getMessage());
    }

    @Test
    @DisplayName("해상도가 4K 면 VIDEO_RESOLUTION_INVALID")
    void should_throw_when_resolution_unsupported() {
        VideoGenerationRequest request = new VideoGenerationRequest("p", 5, "4k");

        assertThatThrownBy(() -> calculator.estimateCostUsd(request, VideoModelTier.SORA))
                .isInstanceOf(VideoException.class)
                .hasMessageContaining(ErrorCode.VIDEO_RESOLUTION_INVALID.getMessage());
    }
}
