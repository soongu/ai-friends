package kr.spartaclub.aifriends.chat.privacy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAnonymizerTest {

    private final UserAnonymizer anonymizer = new UserAnonymizer();

    @Test
    @DisplayName("anonymize() — userId 를 'user_{id}' 형태의 별칭으로 치환한다")
    void anonymize_returnsPrefixedAlias() {
        assertThat(anonymizer.anonymize(1L)).isEqualTo("user_1");
        assertThat(anonymizer.anonymize(42L)).isEqualTo("user_42");
    }
}
