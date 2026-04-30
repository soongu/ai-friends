package kr.spartaclub.aifriends.video.controller;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.common.response.ApiResponse;
import kr.spartaclub.aifriends.video.client.VideoPollingClient;
import kr.spartaclub.aifriends.video.dto.VideoGenerationRequest;
import kr.spartaclub.aifriends.video.dto.VideoJob;
import kr.spartaclub.aifriends.video.dto.VideoModelTier;
import kr.spartaclub.aifriends.video.exception.VideoException;
import kr.spartaclub.aifriends.video.service.VideoCostCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 10 — *선택 실습용* 비디오 생성 비동기 폴링 컨트롤러.
 *
 * <p>두 엔드포인트로 *세 번째 응답 패턴* (Day 6 SSE · Day 9 binary 다음의 비동기 폴링) 의 결을 손에 박는다.</p>
 *
 * <ul>
 *   <li>{@code POST /api/video/generate-async} — Job 제출, {@code 202 ACCEPTED} 와 함께 jobId 가 들어간 QUEUED 스냅샷 반환</li>
 *   <li>{@code GET  /api/video/status/{jobId}} — 현재 상태 폴링, {@code 200 OK} 응답</li>
 * </ul>
 *
 * <p><b>지갑 보호 게이트</b>: 제출 요청의 예상 비용이 {@code aifriends.video.max-cost-usd-per-request}
 * 를 초과하면 {@link ErrorCode#VIDEO_QUOTA_EXCEEDED} 로 거절한다 — *학생 카드를 보호하는 가드*. 본 컨트롤러는
 * Stub 폴링 클라이언트와 결합되어 *실제 외부 호출은 일어나지 않지만*, 이 가드의 결을 손에 익히는 자리.</p>
 *
 * <p>요청 검증 → 비용 가드 → submit/poll → ApiResponse 래핑 의 4 단 결을 그대로 따른다.
 * 실패 응답은 {@code GlobalExceptionHandler} 가 자동으로 {@code ApiResponse.fail} 로 변환한다.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/video")
public class VideoGenerationAsyncController {

    private static final int MIN_DURATION = 1;
    private static final int MAX_DURATION = 10;

    private final VideoPollingClient videoPollingClient;
    private final VideoCostCalculator videoCostCalculator;
    private final double maxCostUsdPerRequest;

    public VideoGenerationAsyncController(
            VideoPollingClient videoPollingClient,
            VideoCostCalculator videoCostCalculator,
            @Value("${aifriends.video.max-cost-usd-per-request:1.0}") double maxCostUsdPerRequest) {
        this.videoPollingClient = videoPollingClient;
        this.videoCostCalculator = videoCostCalculator;
        this.maxCostUsdPerRequest = maxCostUsdPerRequest;
    }

    @PostMapping("/generate-async")
    public ResponseEntity<ApiResponse<VideoJob>> submit(
            @RequestBody VideoGenerationRequest request,
            @RequestParam(name = "tier", defaultValue = "KLING") VideoModelTier tier) {
        validate(request);

        double estimatedCost = videoCostCalculator.estimateCostUsd(request, tier);
        if (estimatedCost > maxCostUsdPerRequest) {
            log.warn("[VideoGenerationAsync] cost guard tripped: estimated=${}, limit=${}",
                    estimatedCost, maxCostUsdPerRequest);
            throw new VideoException(ErrorCode.VIDEO_QUOTA_EXCEEDED);
        }

        VideoJob job = videoPollingClient.submit(request);
        log.info("[VideoGenerationAsync] submit success: jobId={}, tier={}, estimatedCost=${}",
                job.jobId(), tier, estimatedCost);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(job));
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<ApiResponse<VideoJob>> status(@PathVariable("jobId") String jobId) {
        VideoJob job = videoPollingClient.pollStatus(jobId);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    private void validate(VideoGenerationRequest request) {
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            throw new VideoException(ErrorCode.VIDEO_PROMPT_REQUIRED);
        }
        if (request.durationSeconds() < MIN_DURATION || request.durationSeconds() > MAX_DURATION) {
            throw new VideoException(ErrorCode.VIDEO_DURATION_INVALID);
        }
    }
}
