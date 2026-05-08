package kr.spartaclub.aifriends.tool;

import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.repository.SoulmateRepository;
import kr.spartaclub.aifriends.tool.dto.AffinityInfo;
import kr.spartaclub.aifriends.tool.dto.AffinityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Day 11 Step 4 — {@link AffinityTool} 단위 테스트.
 *
 * <p>읽기 전용 도구의 가장 안전한 결을 검증한다 — Repository 를 모킹해
 * (a) 존재하는 Soulmate 의 score → level 변환,
 * (b) 신규/미존재 soulmateId 의 기본값(낯선 사이) 응답,
 * (c) {@link AffinityLevel#from(int)} 분기 경계값
 * 세 축을 한자리에서 검증한다.</p>
 *
 * <p>Step 2 의 {@code WeatherTool} (stub) · Step 3 의 {@code GameStateTool} (영속화) 와 결을 맞춰
 * "도구 본체는 평범한 자바 메서드" 라는 학습 직관을 다시 한번 확인하는 테스트다.</p>
 */
@ExtendWith(MockitoExtension.class)
class AffinityToolTest {

    @Mock
    private SoulmateRepository soulmateRepository;

    @InjectMocks
    private AffinityTool affinityTool;

    @Test
    @DisplayName("getAffinity - 존재하는 Soulmate 의 score 를 그대로 반환하고 level 을 매핑한다")
    void getAffinity_existingSoulmate_returnsScoreAndLevel() {
        Soulmate soulmate = new Soulmate(
                7L, "FEMALE", "char-01", null, "지유",
                "다정함", "독서", "반말", 60, 3, null, null);
        given(soulmateRepository.findById(7L)).willReturn(Optional.of(soulmate));

        AffinityInfo info = affinityTool.getAffinity(7L);

        assertThat(info.found()).isTrue();
        assertThat(info.soulmateId()).isEqualTo(7L);
        assertThat(info.characterName()).isEqualTo("지유");
        assertThat(info.score()).isEqualTo(60);
        assertThat(info.level()).isEqualTo("단짝");
    }

    @Test
    @DisplayName("getAffinity - 존재하지 않는 soulmateId 면 found=false + 기본값(score=0, 낯선 사이) 을 반환한다")
    void getAffinity_unknownId_returnsDefault() {
        given(soulmateRepository.findById(999L)).willReturn(Optional.empty());

        AffinityInfo info = affinityTool.getAffinity(999L);

        assertThat(info.found()).isFalse();
        assertThat(info.soulmateId()).isEqualTo(999L);
        assertThat(info.score()).isZero();
        assertThat(info.level()).isEqualTo("낯선 사이");
    }

    @Test
    @DisplayName("AffinityLevel.from - score 경계값(0/24/25/49/50/74/75/100) 을 라벨로 변환한다")
    void affinityLevel_from_boundaries() {
        assertThat(AffinityLevel.from(0).label()).isEqualTo("낯선 사이");
        assertThat(AffinityLevel.from(24).label()).isEqualTo("낯선 사이");
        assertThat(AffinityLevel.from(25).label()).isEqualTo("친구");
        assertThat(AffinityLevel.from(49).label()).isEqualTo("친구");
        assertThat(AffinityLevel.from(50).label()).isEqualTo("단짝");
        assertThat(AffinityLevel.from(74).label()).isEqualTo("단짝");
        assertThat(AffinityLevel.from(75).label()).isEqualTo("연인");
        assertThat(AffinityLevel.from(100).label()).isEqualTo("연인");
    }
}
