package kr.spartaclub.aifriends.video.service;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.video.dto.VideoGenerationRequest;
import kr.spartaclub.aifriends.video.dto.VideoModelTier;
import kr.spartaclub.aifriends.video.exception.VideoException;
import org.springframework.stereotype.Component;

/**
 * Day 10 — 비디오 생성 비용 계산기 (USD).
 *
 * <p>학생이 *호출 버튼을 누르기 전에* 0 의 개수를 한 번 더 세보게 만드는 자리. 다음 식으로 계산한다:</p>
 *
 * <pre>
 *   cost = pricePerSecond × durationSeconds × resolutionMultiplier
 * </pre>
 *
 * <ul>
 *   <li>{@code pricePerSecond} — {@link VideoModelTier} 의 1초당 단가 (USD)</li>
 *   <li>{@code durationSeconds} — 요청의 길이 (1~10 초)</li>
 *   <li>{@code resolutionMultiplier} — 480p=1×, 720p=2×, 1080p=4×</li>
 * </ul>
 *
 * <p>실제 가격은 모델 정책 변경 · 요청 워크플로(t2v · i2v) · 프레임 레이트에 따라
 * 출렁이므로 본 계산기 결과는 *비용 감각 배양용 추정치* 다 — 청구서 그대로의 수치가 아니다.</p>
 */
@Component
public class VideoCostCalculator {

    /** 480p 해상도 가산 계수. */
    private static final double MULTIPLIER_480P = 1.0;
    /** 720p 해상도 가산 계수. */
    private static final double MULTIPLIER_720P = 2.0;
    /** 1080p 해상도 가산 계수. */
    private static final double MULTIPLIER_1080P = 4.0;

    /**
     * 주어진 요청 + 모델 티어 조합의 예상 비용 (USD) 을 돌려준다.
     *
     * @param request 생성 요청 (durationSeconds, resolution 사용)
     * @param tier    선택한 모델 티어
     * @return 예상 비용 (USD), 소수점 둘째 자리 반올림
     * @throws VideoException 길이/해상도가 허용 범위 밖일 때
     */
    public double estimateCostUsd(VideoGenerationRequest request, VideoModelTier tier) {
        if (request.durationSeconds() < 1 || request.durationSeconds() > 10) {
            throw new VideoException(ErrorCode.VIDEO_DURATION_INVALID);
        }
        double resolutionMultiplier = resolveResolutionMultiplier(request.resolution());
        double raw = tier.getPricePerSecondUsd()
                * request.durationSeconds()
                * resolutionMultiplier;
        return Math.round(raw * 100.0) / 100.0;
    }

    private double resolveResolutionMultiplier(String resolution) {
        if (resolution == null) {
            throw new VideoException(ErrorCode.VIDEO_RESOLUTION_INVALID);
        }
        return switch (resolution) {
            case "480p" -> MULTIPLIER_480P;
            case "720p" -> MULTIPLIER_720P;
            case "1080p" -> MULTIPLIER_1080P;
            default -> throw new VideoException(ErrorCode.VIDEO_RESOLUTION_INVALID);
        };
    }
}
