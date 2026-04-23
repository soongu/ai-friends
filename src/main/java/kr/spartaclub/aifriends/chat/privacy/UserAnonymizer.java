package kr.spartaclub.aifriends.chat.privacy;

import org.springframework.stereotype.Component;

/**
 * Day 3 Step 3 — LLM 에 노출할 유저 식별자를 실명·이메일 대신 안전한 별칭으로 치환한다.
 *
 * <p>Day 2 PII 체크리스트 #1 "시스템 프롬프트에 실명 박지 말 것" 의 집결 지점.
 * 프롬프트가 늘어나도 이 한 클래스만 손대면 전사(全社) 익명화 규칙을 일괄 조정할 수 있다.
 * 실무에서는 HMAC · 역추적 가능한 해시를 쓰는 경우가 많지만,
 * 강의 시점엔 단순 prefix 방식으로 개념만 잡는다.</p>
 */
@Component
public class UserAnonymizer {

    public String anonymize(Long userId) {
        return "user_" + userId;
    }
}
