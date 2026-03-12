package kr.spartaclub.aifriends.service;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.domain.SoulmateAchievement;
import kr.spartaclub.aifriends.dto.SoulmateCreateRequest;
import kr.spartaclub.aifriends.dto.SoulmateProfileResponse;
import kr.spartaclub.aifriends.dto.SoulmateResponse;
import kr.spartaclub.aifriends.repository.SoulmateAchievementRepository;
import kr.spartaclub.aifriends.repository.SoulmateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoulmateServiceTest {

    @Mock
    private SoulmateRepository soulmateRepository;

    @Mock
    private SoulmateAchievementRepository achievementRepository;

    @InjectMocks
    private SoulmateService soulmateService;

    @Test
    @DisplayName("이성친구 생성 성공")
    void createSoulmate_success() {
        // given
        SoulmateCreateRequest request = new SoulmateCreateRequest(
                "FEMALE", "img1", "url", "Alice",
                List.of("kind"), List.of("reading"), List.of("gentle")
        );

        Soulmate savedMock = new Soulmate(1L, "FEMALE", "img1", "url", "Alice", "kind", "reading", "gentle", 0, 1, null);

        given(soulmateRepository.save(any(Soulmate.class))).willReturn(savedMock);

        // when
        SoulmateResponse response = soulmateService.createSoulmate(request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("이성친구 단건 조회 성공 (뱃지 포함)")
    void getSoulmate_success() {
        // given
        Soulmate soulmate = new Soulmate(1L, "FEMALE", "img1", "url", "Alice", "kind", "reading", "gentle", 0, 1, null);
        given(soulmateRepository.findById(1L)).willReturn(Optional.of(soulmate));

        SoulmateAchievement badge1 = new SoulmateAchievement(10L, 1L, "FIRST_MEET", null);
        SoulmateAchievement badge2 = new SoulmateAchievement(11L, 1L, "LEVEL_UP", null);
        given(achievementRepository.findBySoulmateIdOrderByEarnedAtDesc(1L))
                .willReturn(List.of(badge2, badge1));

        // when
        SoulmateProfileResponse response = soulmateService.getSoulmate(1L);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.badges()).containsExactly("LEVEL_UP", "FIRST_MEET");
    }

    @Test
    @DisplayName("이성친구 단건 조회 실패 - 존재하지 않음")
    void getSoulmate_notFound_throwsException() {
        // given
        given(soulmateRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> soulmateService.getSoulmate(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SOULMATE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("전체 이성친구 목록 조회")
    void getSoulmates_success() {
        // given
        Soulmate soulmate1 = new Soulmate(1L, "FEMALE", "img", null, "A", "k", "h", "s", 0, 1, null);
        Soulmate soulmate2 = new Soulmate(2L, "MALE", "img", null, "B", "k", "h", "s", 0, 1, null);

        given(soulmateRepository.findAll()).willReturn(List.of(soulmate1, soulmate2));

        // when
        List<SoulmateResponse> responses = soulmateService.getSoulmates();

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(1).id()).isEqualTo(2L);
    }
}
