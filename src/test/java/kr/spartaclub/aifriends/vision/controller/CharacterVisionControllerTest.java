package kr.spartaclub.aifriends.vision.controller;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.common.exception.GlobalExceptionHandler;
import kr.spartaclub.aifriends.vision.dto.SoulmateIntroductionResponse;
import kr.spartaclub.aifriends.vision.service.CharacterVisionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CharacterVisionController.class)
@Import(GlobalExceptionHandler.class)
class CharacterVisionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CharacterVisionService characterVisionService;

    @Test
    @DisplayName("정상 호출은 200 + ApiResponse.success + 캐릭터 자기소개를 반환한다")
    void should_return_200_with_introduction_when_success() throws Exception {
        SoulmateIntroductionResponse response = new SoulmateIntroductionResponse(
                1L,
                "Alice",
                "https://image.pollinations.ai/prompt/portrait?model=flux",
                "안녕하세요, 저는 Alice 예요. 책 한 권을 들고 있는 모습이 차분해 보이네요."
        );
        when(characterVisionService.introduce(1L)).thenReturn(response);

        mockMvc.perform(post("/api/vision/characters/{soulmateId}/introduce", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.soulmateId").value(1))
                .andExpect(jsonPath("$.data.name").value("Alice"))
                .andExpect(jsonPath("$.data.introduction")
                        .value("안녕하세요, 저는 Alice 예요. 책 한 권을 들고 있는 모습이 차분해 보이네요."));
    }

    @Test
    @DisplayName("서비스가 SOULMATE_NOT_FOUND 를 던지면 404 + S001 로 응답한다")
    void should_return_404_when_soulmate_not_found() throws Exception {
        when(characterVisionService.introduce(99L))
                .thenThrow(new BusinessException(ErrorCode.SOULMATE_NOT_FOUND));

        mockMvc.perform(post("/api/vision/characters/{soulmateId}/introduce", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("S001"));
    }
}
