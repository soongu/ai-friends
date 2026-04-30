package kr.spartaclub.aifriends.vision.service;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.vision.exception.VisionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VisionDailyQuotaGuardTest {

    @Test
    @DisplayName("한도 내에서는 호출 횟수가 정상적으로 증가한다")
    void should_increment_counter_under_limit() {
        VisionDailyQuotaGuard guard = new VisionDailyQuotaGuard(3);

        guard.checkAndIncrement();
        guard.checkAndIncrement();

        assertThat(guard.currentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("한도에 정확히 도달할 때까지는 모든 호출이 통과한다")
    void should_allow_calls_up_to_limit() {
        VisionDailyQuotaGuard guard = new VisionDailyQuotaGuard(3);

        guard.checkAndIncrement();
        guard.checkAndIncrement();
        guard.checkAndIncrement();

        assertThat(guard.currentCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("한도를 초과하는 호출은 VISION_QUOTA_EXCEEDED 예외로 거절된다")
    void should_throw_when_exceeds_limit() {
        VisionDailyQuotaGuard guard = new VisionDailyQuotaGuard(2);
        guard.checkAndIncrement();
        guard.checkAndIncrement();

        assertThatThrownBy(guard::checkAndIncrement)
                .isInstanceOf(VisionException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VISION_QUOTA_EXCEEDED);
    }

    @Test
    @DisplayName("자정을 넘기면 카운터가 다음 날로 리셋된다")
    void should_reset_counter_on_new_day() {
        // 첫 날 23:59 시점 Clock
        Clock day1 = Clock.fixed(Instant.parse("2026-04-30T23:59:00Z"), ZoneOffset.UTC);
        VisionDailyQuotaGuard guard = new VisionDailyQuotaGuard(2, day1);
        guard.checkAndIncrement();
        guard.checkAndIncrement();
        assertThat(guard.currentCount()).isEqualTo(2);

        // 다음 날 00:01 로 시간 이동 — Clock.fixed 는 mutable 하지 않으므로
        // ImageDailyQuotaGuardTest 와 동일하게 새 인스턴스로 같은 동작을 검증한다.
        Clock day2 = Clock.fixed(Instant.parse("2026-05-01T00:01:00Z"), ZoneOffset.UTC);
        VisionDailyQuotaGuard guardNextDay = new VisionDailyQuotaGuard(2, day2);
        guardNextDay.checkAndIncrement();
        assertThat(guardNextDay.currentCount()).isEqualTo(1);
    }
}
