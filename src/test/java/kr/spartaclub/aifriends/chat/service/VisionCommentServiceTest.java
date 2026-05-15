package kr.spartaclub.aifriends.chat.service;

import kr.spartaclub.aifriends.chat.dto.VisionCommentResult;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.vision.exception.VisionException;
import kr.spartaclub.aifriends.vision.service.VisionChatService;
import kr.spartaclub.aifriends.vision.service.VisionDailyQuotaGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

/**
 * Day 8 후속 retrofit — Day 7 {@code SelcaServiceTest} 와 대칭으로 짠 단위 테스트.
 *
 * <p>핵심 책임 두 가지를 확인한다:
 * <ol>
 *   <li>{@code hasImage} — imageUrl 의 null/blank 분기</li>
 *   <li>{@code comment} — Vision 가드 통과 → describe 호출 → 결과 매핑 / 한도 초과 / 빈 응답 fallback</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class VisionCommentServiceTest {

    @Mock
    private VisionChatService visionChatService;

    @Mock
    private VisionDailyQuotaGuard quotaGuard;

    @InjectMocks
    private VisionCommentService visionCommentService;

    private Soulmate sampleSoulmate() {
        return new Soulmate(1L, "MALE", "male-cheerful", null, "현우",
                "다정함, 유머러스", "카페 투어, 야구", "친근한 반말", 0, 1, null, null);
    }

    @Test
    @DisplayName("hasImage — null 또는 공백이면 false, 값이 있으면 true")
    void hasImage_branches() {
        assertThat(visionCommentService.hasImage(null)).isFalse();
        assertThat(visionCommentService.hasImage("")).isFalse();
        assertThat(visionCommentService.hasImage("   ")).isFalse();
        assertThat(visionCommentService.hasImage("/uploads/portraits/upload-1.png")).isTrue();
        assertThat(visionCommentService.hasImage("https://example.com/cat.jpg")).isTrue();
    }

    @Test
    @DisplayName("comment 성공 — 가드 통과 + describe 응답 텍스트를 그대로 success 로 감싼다")
    void comment_success() {
        // given
        Soulmate soulmate = sampleSoulmate();
        given(visionChatService.describe(eq("/uploads/portraits/upload-1.png"), anyString()))
                .willReturn("귀여운 강아지 사진이네! 보고만 있어도 기분이 좋아져.");

        // when
        VisionCommentResult result = visionCommentService.comment(
                soulmate, "/uploads/portraits/upload-1.png", "이거 봐, 우리 강아지!");

        // then
        assertThat(result.aiComment()).isEqualTo("귀여운 강아지 사진이네! 보고만 있어도 기분이 좋아져.");
        assertThat(result.fallbackMessage()).isNull();
        assertThat(result.quotaExceeded()).isFalse();
        then(quotaGuard).should().checkAndIncrement();
    }

    @Test
    @DisplayName("comment — 가드가 VISION_QUOTA_EXCEEDED 던지면 캐릭터 인격 우회 메시지 + quotaExceeded=true, describe 미호출")
    void comment_quotaExceeded() {
        // given
        Soulmate soulmate = sampleSoulmate();
        willThrow(new VisionException(ErrorCode.VISION_QUOTA_EXCEEDED))
                .given(quotaGuard).checkAndIncrement();

        // when
        VisionCommentResult result = visionCommentService.comment(
                soulmate, "/uploads/portraits/upload-1.png", "이거 봐!");

        // then
        assertThat(result.aiComment()).isNull();
        assertThat(result.fallbackMessage()).contains("오늘"); // 캐릭터 인격 톤 (눈 피곤/내일)
        assertThat(result.quotaExceeded()).isTrue();
        then(visionChatService).should(never()).describe(anyString(), anyString());
    }

    @Test
    @DisplayName("comment — describe 가 빈 문자열을 반환하면 failed fallback")
    void comment_emptyDescribe_fallback() {
        // given
        Soulmate soulmate = sampleSoulmate();
        given(visionChatService.describe(anyString(), anyString())).willReturn("");

        // when
        VisionCommentResult result = visionCommentService.comment(
                soulmate, "/uploads/portraits/upload-1.png", "이거 봐!");

        // then
        assertThat(result.aiComment()).isNull();
        assertThat(result.fallbackMessage()).isNotBlank();
        assertThat(result.quotaExceeded()).isFalse();
    }

    @Test
    @DisplayName("comment — describe 가 예외를 던지면 failed fallback (캐릭터 인격 톤)")
    void comment_describeThrows_fallback() {
        // given
        Soulmate soulmate = sampleSoulmate();
        given(visionChatService.describe(anyString(), anyString()))
                .willThrow(new RuntimeException("upstream timeout"));

        // when
        VisionCommentResult result = visionCommentService.comment(
                soulmate, "/uploads/portraits/upload-1.png", "이거 봐!");

        // then
        assertThat(result.aiComment()).isNull();
        assertThat(result.fallbackMessage()).isNotBlank();
        assertThat(result.quotaExceeded()).isFalse();
    }

    @Test
    @DisplayName("comment — userMessage 가 비어도 캐릭터 컨텍스트로 prompt 합성하여 호출")
    void comment_blankUserMessage_stillCallsDescribe() {
        // given
        Soulmate soulmate = sampleSoulmate();
        given(visionChatService.describe(anyString(), anyString())).willReturn("좋은 사진이네!");

        // when
        VisionCommentResult result = visionCommentService.comment(
                soulmate, "/uploads/portraits/upload-1.png", "");

        // then
        assertThat(result.aiComment()).isEqualTo("좋은 사진이네!");
        then(visionChatService).should().describe(eq("/uploads/portraits/upload-1.png"), anyString());
    }
}
