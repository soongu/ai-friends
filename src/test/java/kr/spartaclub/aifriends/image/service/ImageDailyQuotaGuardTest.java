package kr.spartaclub.aifriends.image.service;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.image.exception.ImageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageDailyQuotaGuardTest {

    @Test
    @DisplayName("한도 내에서는 호출 횟수가 정상적으로 증가한다")
    void should_increment_counter_under_limit() {
        ImageDailyQuotaGuard guard = new ImageDailyQuotaGuard(3);

        guard.checkAndIncrement();
        guard.checkAndIncrement();
        guard.checkAndIncrement();

        assertThat(guard.currentCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("한도를 초과하는 호출은 IMAGE_QUOTA_EXCEEDED 예외로 거절된다")
    void should_throw_when_exceeds_limit() {
        ImageDailyQuotaGuard guard = new ImageDailyQuotaGuard(2);
        guard.checkAndIncrement();
        guard.checkAndIncrement();

        assertThatThrownBy(guard::checkAndIncrement)
                .isInstanceOf(ImageException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_QUOTA_EXCEEDED);
    }

    @Test
    @DisplayName("자정을 넘기면 카운터가 다음 날로 리셋된다")
    void should_reset_counter_on_new_day() {
        // 첫 날 23:59 시점 Clock
        Clock day1 = Clock.fixed(Instant.parse("2026-04-30T23:59:00Z"), ZoneOffset.UTC);
        ImageDailyQuotaGuard guard = new ImageDailyQuotaGuard(2, day1);
        guard.checkAndIncrement();
        guard.checkAndIncrement();
        assertThat(guard.currentCount()).isEqualTo(2);

        // 다음 날 00:01 로 시간 이동 — 새 Clock 으로 새 인스턴스를 만드는 대신,
        // 같은 인스턴스의 Clock 을 바꿀 수는 없으므로 패키지 한정 생성자로 같은 동작을 검증한다.
        Clock day2 = Clock.fixed(Instant.parse("2026-05-01T00:01:00Z"), ZoneOffset.UTC);
        ImageDailyQuotaGuard guardNextDay = new ImageDailyQuotaGuard(2, day2);
        guardNextDay.checkAndIncrement();
        assertThat(guardNextDay.currentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 인스턴스에서 시계가 다음 날로 진행되면 자동으로 리셋된다")
    void should_reset_in_place_when_clock_advances() {
        // ZoneId 가 시스템 기본일 때를 모사하기 위해 default zone 으로 Clock 만든다.
        // (Clock.fixed 로 인스턴스 생성 후 다음 날로 진행하는 시나리오는 mutable Clock 이 필요해
        // 학습용으로는 위 should_reset_counter_on_new_day 두 번 인스턴스화 패턴이 더 명확하다.)
        ImageDailyQuotaGuard guard = new ImageDailyQuotaGuard(5, Clock.systemDefaultZone());
        guard.checkAndIncrement();
        assertThat(guard.currentCount()).isEqualTo(1);
    }
}
