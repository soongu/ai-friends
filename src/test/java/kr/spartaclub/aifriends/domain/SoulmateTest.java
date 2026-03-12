package kr.spartaclub.aifriends.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SoulmateTest {

    @Test
    @DisplayName("Soulmate 엔티티가 정상적으로 생성되어야 한다")
    void createSoulmate() {
        // given & when
        Soulmate soulmate = new Soulmate(
                null,
                "FEMALE",
                "char01",
                "http://example.com/img.png",
                "Alice",
                "친절함",
                "독서",
                "다정함",
                0,
                1,
                null
        );

        // then
        assertThat(soulmate.getGender()).isEqualTo("FEMALE");
        assertThat(soulmate.getCharacterImageId()).isEqualTo("char01");
        assertThat(soulmate.getName()).isEqualTo("Alice");
        assertThat(soulmate.getAffectionScore()).isEqualTo(0);
        assertThat(soulmate.getLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("호감도를 추가하면 기존 호감도에 더해진다")
    void addAffection_increasesScore() {
        // given
        Soulmate soulmate = new Soulmate(1L, "MALE", "img", null, null, "x", "y", "z", 10, 1, null);

        // when
        soulmate.addAffection(5);

        // then
        assertThat(soulmate.getAffectionScore()).isEqualTo(15);
    }

    @Test
    @DisplayName("호감도를 감소시켜도 0 미만으로 떨어지지 않는다")
    void addAffection_doesNotDropBelowZero() {
        // given
        Soulmate soulmate = new Soulmate(1L, "MALE", "img", null, null, "x", "y", "z", 3, 1, null);

        // when
        soulmate.addAffection(-5);

        // then
        assertThat(soulmate.getAffectionScore()).isEqualTo(0);
    }

    @Test
    @DisplayName("레벨을 설정하면 정상 반영되며, 상한(10)과 하한(1)을 준수해야 한다")
    void setLevel_boundaries() {
        // given
        Soulmate soulmate = new Soulmate(1L, "MALE", "img", null, null, "x", "y", "z", 0, 1, null);

        // when & then: 정상 레벨
        soulmate.setLevel(5);
        assertThat(soulmate.getLevel()).isEqualTo(5);

        // when & then: 상한 초과
        soulmate.setLevel(15);
        assertThat(soulmate.getLevel()).isEqualTo(10);

        // when & then: 하한 미달
        soulmate.setLevel(-5);
        assertThat(soulmate.getLevel()).isEqualTo(1);
    }
}
