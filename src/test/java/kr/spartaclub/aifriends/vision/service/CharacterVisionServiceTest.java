package kr.spartaclub.aifriends.vision.service;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.repository.SoulmateRepository;
import kr.spartaclub.aifriends.vision.dto.SoulmateIntroductionResponse;
import kr.spartaclub.aifriends.vision.exception.VisionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Day 8 Step 6 — CharacterVisionService 단위 테스트.
 *
 * <p>외부 프로바이더 호출은 전부 모킹. {@link VisionChatService} · {@link SoulmateRepository}
 * 두 협력자만 Mockito 로 잡고, 캐릭터 컨텍스트 → portrait URL → Vision 응답까지의
 * 흐름을 검증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class CharacterVisionServiceTest {

    @Mock
    SoulmateRepository soulmateRepository;

    @Mock
    VisionChatService visionChatService;

    @Mock
    VisionDailyQuotaGuard quotaGuard;

    @InjectMocks
    CharacterVisionService characterVisionService;

    @Test
    @DisplayName("캐릭터 + portrait URL 정상이면 VisionChatService.describe 호출 + 응답 텍스트가 introduction 에 박힌다")
    void should_return_introduction_when_soulmate_has_portrait() {
        // given
        String portraitUrl = "https://image.pollinations.ai/prompt/portrait?model=flux";
        Soulmate soulmate = new Soulmate(
                1L, "FEMALE", "img1", portraitUrl, "Alice",
                "차분함, 다정함", "독서, 산책", "존댓말",
                0, 1, LocalDateTime.now(), null);
        when(soulmateRepository.findById(1L)).thenReturn(Optional.of(soulmate));
        when(visionChatService.describe(eq(portraitUrl), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("안녕하세요, 저는 Alice 예요. 책 한 권을 들고 있는 모습이 차분해 보이네요.");

        // when
        SoulmateIntroductionResponse response = characterVisionService.introduce(1L);

        // then
        verify(visionChatService).describe(eq(portraitUrl), org.mockito.ArgumentMatchers.contains("Alice"));
        assertThat(response.soulmateId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Alice");
        assertThat(response.portraitUrl()).isEqualTo(portraitUrl);
        assertThat(response.introduction())
                .isEqualTo("안녕하세요, 저는 Alice 예요. 책 한 권을 들고 있는 모습이 차분해 보이네요.");
    }

    @Test
    @DisplayName("캐릭터를 찾을 수 없으면 BusinessException(SOULMATE_NOT_FOUND) 을 던진다")
    void should_throw_business_exception_when_soulmate_not_found() {
        when(soulmateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> characterVisionService.introduce(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SOULMATE_NOT_FOUND);
    }

    @Test
    @DisplayName("portrait URL 이 null 이면 VisionException(VISION_PORTRAIT_NOT_AVAILABLE) 을 던진다")
    void should_throw_vision_exception_when_portrait_url_null() {
        Soulmate soulmate = new Soulmate(
                1L, "FEMALE", "img1", null, "Alice",
                "차분함", "독서", "존댓말",
                0, 1, LocalDateTime.now(), null);
        when(soulmateRepository.findById(1L)).thenReturn(Optional.of(soulmate));

        assertThatThrownBy(() -> characterVisionService.introduce(1L))
                .isInstanceOf(VisionException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VISION_PORTRAIT_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("portrait URL 이 빈 문자열이면 VisionException(VISION_PORTRAIT_NOT_AVAILABLE) 을 던진다")
    void should_throw_vision_exception_when_portrait_url_blank() {
        Soulmate soulmate = new Soulmate(
                1L, "FEMALE", "img1", "", "Alice",
                "차분함", "독서", "존댓말",
                0, 1, LocalDateTime.now(), null);
        when(soulmateRepository.findById(1L)).thenReturn(Optional.of(soulmate));

        assertThatThrownBy(() -> characterVisionService.introduce(1L))
                .isInstanceOf(VisionException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VISION_PORTRAIT_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("일일 호출 한도 초과 시 VISION_QUOTA_EXCEEDED 예외가 그대로 propagate 되고, soulmateRepository 는 호출되지 않는다")
    void should_propagate_quota_exceeded_and_skip_repository() {
        doThrow(new VisionException(ErrorCode.VISION_QUOTA_EXCEEDED))
                .when(quotaGuard).checkAndIncrement();

        assertThatThrownBy(() -> characterVisionService.introduce(1L))
                .isInstanceOf(VisionException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VISION_QUOTA_EXCEEDED);

        verifyNoInteractions(soulmateRepository);
        verifyNoInteractions(visionChatService);
    }
}
