package kr.spartaclub.aifriends.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;

class SoulmateAchievementTest {

    @Test
    @DisplayName("SoulmateAchievement 엔티티가 정상적으로 생성되어야 한다")
    void createSoulmateAchievement() {
        // given & when
        SoulmateAchievement achievement = new SoulmateAchievement(null, 100L, "FIRST_MEET", null);

        // then
        assertThat(achievement.getSoulmateId()).isEqualTo(100L);
        assertThat(achievement.getBadgeCode()).isEqualTo("FIRST_MEET");
    }
}
