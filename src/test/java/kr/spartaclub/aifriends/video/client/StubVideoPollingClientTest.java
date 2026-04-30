package kr.spartaclub.aifriends.video.client;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.video.dto.VideoGenerationRequest;
import kr.spartaclub.aifriends.video.dto.VideoJob;
import kr.spartaclub.aifriends.video.dto.VideoJobStatus;
import kr.spartaclub.aifriends.video.exception.VideoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Day 10 — Stub 폴링 클라이언트의 시간 흐름 시뮬레이션 검증.
 *
 * <p>{@link Clock} 을 직접 주입해 시간을 얼리고, {@code submit} 직후 / +2 초 후 / +5 초 후의
 * 상태가 QUEUED → RUNNING → SUCCEEDED 로 흐르는지 확인한다.</p>
 */
class StubVideoPollingClientTest {

    private final VideoGenerationRequest request =
            new VideoGenerationRequest("a cat dancing in space", 5, "720p");

    @Test
    @DisplayName("submit 직후엔 QUEUED + jobId 가 채워진다")
    void should_return_queued_after_submit() {
        StubVideoPollingClient client = new StubVideoPollingClient(fixedClockAt(0));

        VideoJob job = client.submit(request);

        assertThat(job.status()).isEqualTo(VideoJobStatus.QUEUED);
        assertThat(job.jobId()).isNotBlank();
        assertThat(job.videoUrl()).isNull();
    }

    @Test
    @DisplayName("submit 시각 그대로 폴링하면 QUEUED")
    void should_return_queued_when_polled_immediately() {
        Clock clock = fixedClockAt(0);
        StubVideoPollingClient client = new StubVideoPollingClient(clock);

        VideoJob submitted = client.submit(request);
        VideoJob polled = client.pollStatus(submitted.jobId());

        assertThat(polled.status()).isEqualTo(VideoJobStatus.QUEUED);
    }

    @Test
    @DisplayName("submit 후 3 초가 지나면 RUNNING")
    void should_return_running_after_3_seconds() {
        AdvanceableClock clock = new AdvanceableClock(Instant.parse("2026-04-30T00:00:00Z"));
        StubVideoPollingClient client = new StubVideoPollingClient(clock);

        VideoJob submitted = client.submit(request);
        clock.advance(Duration.ofSeconds(3));
        VideoJob polled = client.pollStatus(submitted.jobId());

        assertThat(polled.status()).isEqualTo(VideoJobStatus.RUNNING);
        assertThat(polled.videoUrl()).isNull();
    }

    @Test
    @DisplayName("submit 후 6 초가 지나면 SUCCEEDED + videoUrl 채움")
    void should_return_succeeded_after_6_seconds() {
        AdvanceableClock clock = new AdvanceableClock(Instant.parse("2026-04-30T00:00:00Z"));
        StubVideoPollingClient client = new StubVideoPollingClient(clock);

        VideoJob submitted = client.submit(request);
        clock.advance(Duration.ofSeconds(6));
        VideoJob polled = client.pollStatus(submitted.jobId());

        assertThat(polled.status()).isEqualTo(VideoJobStatus.SUCCEEDED);
        assertThat(polled.videoUrl()).contains(submitted.jobId());
    }

    @Test
    @DisplayName("존재하지 않는 jobId 로 폴링하면 VIDEO_JOB_NOT_FOUND")
    void should_throw_when_job_id_unknown() {
        StubVideoPollingClient client = new StubVideoPollingClient(fixedClockAt(0));

        assertThatThrownBy(() -> client.pollStatus("not-exist"))
                .isInstanceOf(VideoException.class)
                .hasMessageContaining(ErrorCode.VIDEO_JOB_NOT_FOUND.getMessage());
    }

    private static Clock fixedClockAt(long epochSeconds) {
        return Clock.fixed(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
    }

    /** 테스트 한정 — 현재 시간을 호출자가 손으로 흘려보낼 수 있는 Clock. */
    private static final class AdvanceableClock extends Clock {
        private Instant now;

        AdvanceableClock(Instant initial) {
            this.now = initial;
        }

        void advance(Duration duration) {
            this.now = this.now.plus(duration);
        }

        @Override public ZoneId getZone() { return ZoneId.systemDefault(); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
