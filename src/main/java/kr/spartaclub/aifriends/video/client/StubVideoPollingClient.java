package kr.spartaclub.aifriends.video.client;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.video.dto.VideoGenerationRequest;
import kr.spartaclub.aifriends.video.dto.VideoJob;
import kr.spartaclub.aifriends.video.dto.VideoJobStatus;
import kr.spartaclub.aifriends.video.exception.VideoException;
import org.springframework.stereotype.Component;

/**
 * Day 10 — 비동기 폴링의 *손맛* 을 학생 실습에서 안전하게 흉내내는 Stub 구현체.
 *
 * <p>실제 Veo 3 · Sora · Runway 호출 없이 *지갑이 안전한* 상태로 폴링 패턴을 손에 박는 자리.
 * 동작은 다음과 같다:</p>
 *
 * <ol>
 *   <li>{@code submit} → 새 jobId 부여, {@code QUEUED} 로 인메모리 Map 에 박음, 제출 시각 기록</li>
 *   <li>{@code pollStatus} 호출 시점에 (현재시각 - 제출시각) 으로 다음을 결정
 *     <ul>
 *       <li>+0 ~ +2 초 미만 → {@code QUEUED}</li>
 *       <li>+2 ~ +5 초 미만 → {@code RUNNING}</li>
 *       <li>+5 초 이상     → {@code SUCCEEDED} (videoUrl 채움)</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p>실제 외부 API 라면 분 단위가 걸리는 자리지만 *학생 실습용 시뮬레이션* 이라
 * 5 초 안에 완료되는 결로 가속해 박았다 — 폴링 흐름의 결을 손에 익히는 게 목적이지
 * 분 단위 대기를 학생에게 시키는 게 목적이 아니다.</p>
 *
 * <p>{@link Clock} 을 주입받아 테스트에서 시간 흐름을 결정론적으로 컨트롤한다.</p>
 */
@Component
public class StubVideoPollingClient implements VideoPollingClient {

    /** QUEUED → RUNNING 전환까지의 가상 대기 시간. */
    private static final Duration QUEUED_DURATION = Duration.ofSeconds(2);

    /** RUNNING → SUCCEEDED 전환까지의 가상 추론 시간 (제출 시각 기준 누적). */
    private static final Duration RUNNING_DURATION = Duration.ofSeconds(5);

    private final Clock clock;
    private final ConcurrentMap<String, Instant> submittedAtByJobId = new ConcurrentHashMap<>();

    public StubVideoPollingClient() {
        this(Clock.systemDefaultZone());
    }

    /** 테스트 한정 — Clock 을 외부에서 주입해 폴링 시간 경계를 결정론적으로 검증한다. */
    StubVideoPollingClient(Clock clock) {
        this.clock = clock;
    }

    @Override
    public VideoJob submit(VideoGenerationRequest request) {
        String jobId = UUID.randomUUID().toString();
        submittedAtByJobId.put(jobId, Instant.now(clock));
        return VideoJob.queued(jobId);
    }

    @Override
    public VideoJob pollStatus(String jobId) {
        Instant submittedAt = submittedAtByJobId.get(jobId);
        if (submittedAt == null) {
            throw new VideoException(ErrorCode.VIDEO_JOB_NOT_FOUND);
        }

        Duration elapsed = Duration.between(submittedAt, Instant.now(clock));

        if (elapsed.compareTo(QUEUED_DURATION) < 0) {
            return new VideoJob(jobId, VideoJobStatus.QUEUED, null, null);
        }
        if (elapsed.compareTo(RUNNING_DURATION) < 0) {
            return new VideoJob(jobId, VideoJobStatus.RUNNING, null, null);
        }
        return new VideoJob(jobId, VideoJobStatus.SUCCEEDED,
                "https://stub.local/videos/" + jobId + ".mp4", null);
    }
}
