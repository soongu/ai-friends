package kr.spartaclub.aifriends.video.dto;

/**
 * Day 10 — 비디오 생성 모델별 티어와 1초당 기본 가격 (USD).
 *
 * <p>2026-04 기준 *공식 가격표를 단순화한 강의용 추정치* 다. 실제 청구액은 모델 정책 변경 ·
 * 해상도 가산 · 워크플로(t2v · i2v) 에 따라 출렁이므로, 본 enum 의 수치는 *비용 감각 배양용*
 * 으로만 쓴다 — *학생이 본인 카드로 호출하기 전에 0 을 한 번 더 세는 결* 을 노린다.</p>
 *
 * <p>해상도 가산 계수는 {@link kr.spartaclub.aifriends.video.service.VideoCostCalculator}
 * 에서 곱한다 (480p=1×, 720p=2×, 1080p=4×).</p>
 */
public enum VideoModelTier {

    /** 로컬 GPU 무료 대안 — Stable Video Diffusion. */
    STABLE_VIDEO_DIFFUSION_LOCAL(0.00),

    /** Kuaishou Kling — 가성비 + 길이 강점. */
    KLING(0.06),

    /** Luma Dream Machine — 빠른 생성 + 중간 가격대. */
    LUMA_DREAM_MACHINE(0.07),

    /** Runway Gen-4 — 영상 업계 디폴트. */
    RUNWAY_GEN_4(0.10),

    /** Google Veo 3 — Gemini 패밀리, 사운드 포함 비디오. */
    VEO_3(0.50),

    /** OpenAI Sora — 최고 품질 + 최고 가격. */
    SORA(1.00);

    private final double pricePerSecondUsd;

    VideoModelTier(double pricePerSecondUsd) {
        this.pricePerSecondUsd = pricePerSecondUsd;
    }

    public double getPricePerSecondUsd() {
        return pricePerSecondUsd;
    }
}
