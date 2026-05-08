package kr.spartaclub.aifriends.service;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.CharacterPreset;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.domain.SoulmateAchievement;
import kr.spartaclub.aifriends.dto.SoulmateCreateRequest;
import kr.spartaclub.aifriends.dto.SoulmateProfileResponse;
import kr.spartaclub.aifriends.dto.SoulmateResponse;
import kr.spartaclub.aifriends.image.dto.ImageGenerationResult;
import kr.spartaclub.aifriends.image.service.ImageGenerationService;
import kr.spartaclub.aifriends.repository.SoulmateAchievementRepository;
import kr.spartaclub.aifriends.repository.SoulmateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SoulmateServiceTest {

    @Mock
    private SoulmateRepository soulmateRepository;

    @Mock
    private SoulmateAchievementRepository achievementRepository;

    @Mock
    private ImageGenerationService imageGenerationService;

    @InjectMocks
    private SoulmateService soulmateService;

    @Test
    @DisplayName("프리셋 트랙 — male-cheerful 선택 시 enum 메타의 appearancePrompt 가 박힘 + ImageGenerationService 미호출")
    void createSoulmate_preset_male_cheerful_uses_enum_prompt() {
        // given
        SoulmateCreateRequest request = new SoulmateCreateRequest(
                "MALE", "male-cheerful",
                "/static/images/characters/character-male-cheerful-face.jpg",
                "Alex",
                List.of("kind"), List.of("reading"), List.of("gentle"),
                null  // 커스텀 외모 prompt — 프리셋 트랙이라 null
        );

        ArgumentCaptor<Soulmate> entityCaptor = ArgumentCaptor.forClass(Soulmate.class);
        Soulmate savedMock = new Soulmate(
                1L, "MALE", "male-cheerful",
                "/static/images/characters/character-male-cheerful-face.jpg",
                "Alex", "kind", "reading", "gentle", 0, 1, null,
                CharacterPreset.MALE_CHEERFUL.getAppearancePrompt()
        );
        given(soulmateRepository.save(any(Soulmate.class))).willReturn(savedMock);

        // when
        SoulmateResponse response = soulmateService.createSoulmate(request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.appearancePrompt())
                .isEqualTo(CharacterPreset.MALE_CHEERFUL.getAppearancePrompt());

        // 프리셋 트랙은 ImageGenerationService 를 호출하지 않는다 (비용 0)
        verify(imageGenerationService, never())
                .generate(anyString(), anyString(), any(), anyString());

        // 저장된 엔티티의 appearancePrompt 검증
        verify(soulmateRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getAppearancePrompt())
                .isEqualTo(CharacterPreset.MALE_CHEERFUL.getAppearancePrompt());
    }

    @Test
    @DisplayName("프리셋 트랙 — female-bright 선택 시 enum 메타의 appearancePrompt 가 박힘")
    void createSoulmate_preset_female_bright_uses_enum_prompt() {
        // given
        SoulmateCreateRequest request = new SoulmateCreateRequest(
                "FEMALE", "female-bright",
                "/static/images/characters/character-female-bright-face.jpg",
                "Riko",
                List.of("kind"), List.of("reading"), List.of("gentle"),
                null
        );

        Soulmate savedMock = new Soulmate(
                2L, "FEMALE", "female-bright",
                "/static/images/characters/character-female-bright-face.jpg",
                "Riko", "kind", "reading", "gentle", 0, 1, null,
                CharacterPreset.FEMALE_BRIGHT.getAppearancePrompt()
        );
        given(soulmateRepository.save(any(Soulmate.class))).willReturn(savedMock);

        // when
        SoulmateResponse response = soulmateService.createSoulmate(request);

        // then
        assertThat(response.appearancePrompt())
                .isEqualTo(CharacterPreset.FEMALE_BRIGHT.getAppearancePrompt());
    }

    @Test
    @DisplayName("커스텀 트랙 — ImageGenerationService 1회 호출 + 결과 localPath 가 characterImageUrl 에 박힘")
    void createSoulmate_custom_calls_image_generation_and_uses_local_path() {
        // given
        String customPrompt = "20대 초반 한국인 여성, 안경 쓴 차분한 인상, anime portrait illustration";
        SoulmateCreateRequest request = new SoulmateCreateRequest(
                "FEMALE", CharacterPreset.CUSTOM_IMAGE_ID, null, "Mia",
                List.of("kind"), List.of("reading"), List.of("gentle"),
                customPrompt
        );

        ImageGenerationResult genResult = new ImageGenerationResult(
                "/uploads/portraits/soulmate-portrait-abc.jpg",
                "https://image.pollinations.ai/prompt/...",
                customPrompt,
                "pollinations-flux",
                0.0
        );
        given(imageGenerationService.generate(eq(customPrompt), isNull(), isNull(), anyString()))
                .willReturn(genResult);

        ArgumentCaptor<Soulmate> entityCaptor = ArgumentCaptor.forClass(Soulmate.class);
        Soulmate savedMock = new Soulmate(
                3L, "FEMALE", CharacterPreset.CUSTOM_IMAGE_ID,
                "/uploads/portraits/soulmate-portrait-abc.jpg",
                "Mia", "kind", "reading", "gentle", 0, 1, null,
                customPrompt
        );
        given(soulmateRepository.save(any(Soulmate.class))).willReturn(savedMock);

        // when
        SoulmateResponse response = soulmateService.createSoulmate(request);

        // then
        verify(imageGenerationService).generate(eq(customPrompt), isNull(), isNull(), anyString());

        verify(soulmateRepository).save(entityCaptor.capture());
        Soulmate captured = entityCaptor.getValue();
        assertThat(captured.getCharacterImageUrl()).isEqualTo("/uploads/portraits/soulmate-portrait-abc.jpg");
        assertThat(captured.getAppearancePrompt()).isEqualTo(customPrompt);

        assertThat(response.characterImageUrl()).isEqualTo("/uploads/portraits/soulmate-portrait-abc.jpg");
        assertThat(response.appearancePrompt()).isEqualTo(customPrompt);
    }

    @Test
    @DisplayName("커스텀 트랙 — customAppearancePrompt 가 null 이면 SOULMATE_CUSTOM_PROMPT_REQUIRED 예외")
    void createSoulmate_custom_without_prompt_throws() {
        // given
        SoulmateCreateRequest request = new SoulmateCreateRequest(
                "FEMALE", CharacterPreset.CUSTOM_IMAGE_ID, null, "Mia",
                List.of("kind"), List.of("reading"), List.of("gentle"),
                null  // customAppearancePrompt 누락
        );

        // when & then
        assertThatThrownBy(() -> soulmateService.createSoulmate(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SOULMATE_CUSTOM_PROMPT_REQUIRED.getMessage());

        // 검증 실패 시 ImageGenerationService 가 호출되지 않아야 함 (비용 차단)
        verify(imageGenerationService, never())
                .generate(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("커스텀 트랙 — customAppearancePrompt 가 공백 문자열이어도 SOULMATE_CUSTOM_PROMPT_REQUIRED 예외")
    void createSoulmate_custom_with_blank_prompt_throws() {
        // given
        SoulmateCreateRequest request = new SoulmateCreateRequest(
                "FEMALE", CharacterPreset.CUSTOM_IMAGE_ID, null, "Mia",
                List.of("kind"), List.of("reading"), List.of("gentle"),
                "   "  // 공백만
        );

        // when & then
        assertThatThrownBy(() -> soulmateService.createSoulmate(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SOULMATE_CUSTOM_PROMPT_REQUIRED.getMessage());
    }

    @Test
    @DisplayName("프리셋 트랙 — 잘못된 characterImageId 면 SOULMATE_INVALID_PRESET 예외")
    void createSoulmate_unknown_preset_id_throws() {
        // given
        SoulmateCreateRequest request = new SoulmateCreateRequest(
                "MALE", "unknown-preset-xyz", null, "Alex",
                List.of("kind"), List.of("reading"), List.of("gentle"),
                null
        );

        // when & then
        assertThatThrownBy(() -> soulmateService.createSoulmate(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SOULMATE_INVALID_PRESET.getMessage());

        verify(imageGenerationService, never())
                .generate(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("이성친구 단건 조회 성공 (뱃지 포함)")
    void getSoulmate_success() {
        // given
        Soulmate soulmate = new Soulmate(
                1L, "FEMALE", "female-warm", "url", "Alice",
                "kind", "reading", "gentle", 0, 1, null,
                CharacterPreset.FEMALE_WARM.getAppearancePrompt()
        );
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
        Soulmate soulmate1 = new Soulmate(
                1L, "FEMALE", "female-warm", null, "A",
                "k", "h", "s", 0, 1, null,
                CharacterPreset.FEMALE_WARM.getAppearancePrompt()
        );
        Soulmate soulmate2 = new Soulmate(
                2L, "MALE", "male-calm", null, "B",
                "k", "h", "s", 0, 1, null,
                CharacterPreset.MALE_CALM.getAppearancePrompt()
        );

        given(soulmateRepository.findAll()).willReturn(List.of(soulmate1, soulmate2));

        // when
        List<SoulmateResponse> responses = soulmateService.getSoulmates();

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(1).id()).isEqualTo(2L);
    }
}
