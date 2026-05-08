package kr.spartaclub.aifriends.chat.service;

import kr.spartaclub.aifriends.chat.dto.SelcaResult;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.image.dto.ImageGenerationResult;
import kr.spartaclub.aifriends.image.exception.ImageException;
import kr.spartaclub.aifriends.image.service.ImageGenerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Day 7 Step 9 — SelcaService 단위 테스트.
 *
 * <p>검증 축:
 * <ol>
 *   <li>키워드 매칭 — *"셀카"* / *"selfie"* / *"사진"* / *"셀피"* 가 들어 있으면 true.</li>
 *   <li>외모 일관성 prompt 합성 — appearancePrompt + 사용자 자유 입력 + selfie suffix.</li>
 *   <li>이미지 생성 성공 — localPath 가 SelcaResult.imageUrl 에 박힘.</li>
 *   <li>한도 초과 — quotaExceeded SelcaResult + fallback 메시지.</li>
 *   <li>다른 ImageException — failed SelcaResult + fallback 메시지 (다른 톤).</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class SelcaServiceTest {

    @Mock
    private ImageGenerationService imageGenerationService;

    @InjectMocks
    private SelcaService selcaService;

    @Test
    @DisplayName("isSelcaRequest — *셀카* / *selfie* / *사진* 키워드 매칭")
    void isSelcaRequest_matchesKeywords() {
        assertThat(selcaService.isSelcaRequest("셀카 보내줘")).isTrue();
        assertThat(selcaService.isSelcaRequest("오늘 카페에서 책 읽는 셀카")).isTrue();
        assertThat(selcaService.isSelcaRequest("Send me a selfie")).isTrue();
        assertThat(selcaService.isSelcaRequest("사진 한 장")).isTrue();
        assertThat(selcaService.isSelcaRequest("셀피 좀 줘")).isTrue();
    }

    @Test
    @DisplayName("isSelcaRequest — 셀카 키워드 없으면 false (null/blank 안전)")
    void isSelcaRequest_noKeyword_returnsFalse() {
        assertThat(selcaService.isSelcaRequest("안녕!")).isFalse();
        assertThat(selcaService.isSelcaRequest("오늘 뭐 했어")).isFalse();
        assertThat(selcaService.isSelcaRequest("")).isFalse();
        assertThat(selcaService.isSelcaRequest(null)).isFalse();
    }

    @Test
    @DisplayName("composePrompt — appearancePrompt + 사용자 자유 입력 + selfie suffix")
    void composePrompt_combines_appearance_and_user_request() {
        String prompt = selcaService.composePrompt(
                "20대 후반 한국인 여성, 긴 갈색 머리",
                "오늘 카페에서 책 읽는 셀카 보내줘"
        );

        // 외모 prompt 가 들어 있다
        assertThat(prompt).contains("20대 후반 한국인 여성, 긴 갈색 머리");
        // 사용자 자유 입력에서 *"셀카"* 키워드는 빠지고 나머지가 들어 있다
        assertThat(prompt).contains("카페에서 책 읽는");
        assertThat(prompt).doesNotContain("셀카");
        // selfie suffix 가 마지막에
        assertThat(prompt).endsWith("selfie, casual photo angle");
    }

    @Test
    @DisplayName("composePrompt — 사용자 자유 입력이 *셀카* 키워드만 있을 때도 결과가 비지 않음")
    void composePrompt_only_keyword_returns_appearance_plus_suffix() {
        String prompt = selcaService.composePrompt("외모 묘사", "셀카");

        assertThat(prompt).contains("외모 묘사");
        assertThat(prompt).endsWith("selfie, casual photo angle");
    }

    @Test
    @DisplayName("generate — ImageGenerationService 호출 + localPath 가 SelcaResult.imageUrl 에 박힘")
    void generate_success_returns_imageUrl() {
        // given
        Soulmate soulmate = new Soulmate(
                1L, "FEMALE", "female-warm", "url", "Mia",
                "kind", "reading", "gentle", 0, 1, null,
                "20대 후반 한국인 여성, 긴 갈색 머리, 따뜻한 미소"
        );

        ImageGenerationResult genResult = new ImageGenerationResult(
                "/uploads/portraits/selca-xyz.jpg",
                "https://image.pollinations.ai/...",
                "compose prompt",
                "pollinations-flux",
                0.0
        );
        given(imageGenerationService.generate(anyString(), isNull(), isNull(), anyString()))
                .willReturn(genResult);

        // when
        SelcaResult result = selcaService.generate(soulmate, "오늘 카페에서 책 읽는 셀카");

        // then
        assertThat(result.imageUrl()).isEqualTo("/uploads/portraits/selca-xyz.jpg");
        assertThat(result.fallbackMessage()).isNull();
        assertThat(result.quotaExceeded()).isFalse();

        // generate 호출의 prompt 인자는 외모 prompt + 사용자 자유 입력이 합성된 것
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(imageGenerationService).generate(promptCaptor.capture(), isNull(), isNull(), anyString());
        assertThat(promptCaptor.getValue()).contains("긴 갈색 머리");
        assertThat(promptCaptor.getValue()).contains("카페");
    }

    @Test
    @DisplayName("generate — 한도 초과 시 quotaExceeded SelcaResult + 캐릭터 인격 우회 메시지")
    void generate_quotaExceeded_returns_fallback() {
        // given
        Soulmate soulmate = new Soulmate(
                1L, "FEMALE", "female-warm", "url", "Mia",
                "kind", "reading", "gentle", 0, 1, null,
                "외모"
        );
        given(imageGenerationService.generate(anyString(), any(), any(), anyString()))
                .willThrow(new ImageException(ErrorCode.IMAGE_QUOTA_EXCEEDED));

        // when
        SelcaResult result = selcaService.generate(soulmate, "셀카");

        // then
        assertThat(result.imageUrl()).isNull();
        assertThat(result.quotaExceeded()).isTrue();
        assertThat(result.fallbackMessage()).contains("카메라 배터리");
    }

    @Test
    @DisplayName("generate — 한도 외 다른 이미지 실패는 failed SelcaResult + 다른 fallback 메시지")
    void generate_otherFailure_returns_failed_fallback() {
        // given
        Soulmate soulmate = new Soulmate(
                1L, "FEMALE", "female-warm", "url", "Mia",
                "kind", "reading", "gentle", 0, 1, null,
                "외모"
        );
        given(imageGenerationService.generate(anyString(), any(), any(), anyString()))
                .willThrow(new ImageException(ErrorCode.IMAGE_GENERATION_FAILED));

        // when
        SelcaResult result = selcaService.generate(soulmate, "셀카");

        // then
        assertThat(result.imageUrl()).isNull();
        assertThat(result.quotaExceeded()).isFalse();
        assertThat(result.fallbackMessage()).contains("핸드폰이 말썽");
    }

    @Test
    @DisplayName("isSelcaRequest 가 false 인 메시지는 generate 가 *호출되지 않는다* — AiChatService 분기 책임")
    void isSelcaRequest_false_does_not_invoke_generate() {
        // 이 자리에서 generate 호출 자체를 하지 않는다는 검증은 AiChatServiceTest 가 책임진다.
        // 본 서비스 단위에서는 isSelcaRequest 의 분리 자체만 보장하면 된다.
        assertThat(selcaService.isSelcaRequest("그냥 안녕")).isFalse();
        verify(imageGenerationService, never()).generate(anyString(), anyString(), any(), anyString());
    }
}
