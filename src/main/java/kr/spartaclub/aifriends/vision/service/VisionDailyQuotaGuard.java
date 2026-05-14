package kr.spartaclub.aifriends.vision.service;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.vision.exception.VisionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Day 8 Step 7 — 일일 Vision (멀티모달 입력) 호출 횟수 가드.
 *
 * <p><b>학습용 단순 모양</b>이라 in-memory + synchronized 로 카운터를 관리한다.
 * 실제 운영에서는 다음 두 한계를 반드시 고려해야 한다:
 * <ul>
 *   <li>인스턴스 다중화: WAS 가 N대면 카운터가 N배 부풀려진다 → Redis {@code INCR} + {@code EXPIRE} 로 공유.</li>
 *   <li>유저별 격리: 지금은 "전체 호출" 한도다 → 운영에서는 {@code key = "vision:quota:{userId}:{yyyyMMdd}"} 로 키 분리.</li>
 * </ul>
 * Vision 호출은 텍스트 LLM 1.5~2 배 비용이 들기 때문에 Day 7 {@code ImageDailyQuotaGuard} 보다
 * 한도를 살짝 작게(기본 20) 잡았다. Day 8 의 학습 목표는 "비용이 큰 모달리티에 가드를 끼우는 패턴"
 * 그 자체이므로 이 단순 구현으로 충분하다.</p>
 */
@Component
public class VisionDailyQuotaGuard {

    private final int dailyLimit;
    private final Clock clock;

    private LocalDate currentDate;
    private int counter;

    @Autowired
    public VisionDailyQuotaGuard(@Value("${aifriends.vision.quota.daily-limit:20}") int dailyLimit) {
        this(dailyLimit, Clock.systemDefaultZone());
    }

    /**
     * 테스트 한정 — Clock 을 외부에서 주입해 자정 경계 시나리오를 검증할 수 있게 한다.
     */
    VisionDailyQuotaGuard(int dailyLimit, Clock clock) {
        this.dailyLimit = dailyLimit;
        this.clock = clock;
        this.currentDate = LocalDate.now(clock);
        this.counter = 0;
    }

    /**
     * 호출 횟수를 1 증가시키고, 한도를 넘기면 {@link VisionException} 을 던진다.
     * 자정을 넘기면 카운터가 자동으로 0으로 리셋된다.
     */
    public synchronized void checkAndIncrement() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(currentDate)) {
            currentDate = today;
            counter = 0;
        }
        if (counter + 1 > dailyLimit) {
            throw new VisionException(ErrorCode.VISION_QUOTA_EXCEEDED);
        }
        counter++;
    }

    /**
     * 현재 사용량 (디버깅/관리 엔드포인트 노출용 — 학습용으로 두지만 노출은 신중히).
     */
    public synchronized int currentCount() {
        return counter;
    }
}
