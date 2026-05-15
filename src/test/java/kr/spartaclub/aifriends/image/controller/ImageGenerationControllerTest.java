package kr.spartaclub.aifriends.image.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.common.exception.GlobalExceptionHandler;
import kr.spartaclub.aifriends.image.dto.ImageGenerationResult;
import kr.spartaclub.aifriends.image.dto.PortraitGenerationRequest;
import kr.spartaclub.aifriends.image.exception.ImageException;
import kr.spartaclub.aifriends.image.service.ImageGenerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImageGenerationController.class)
@Import(GlobalExceptionHandler.class)
class ImageGenerationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ImageGenerationService imageGenerationService;

    @Test
    @DisplayName("정상 요청은 200 + ApiResponse.success(localPath, externalUrl) 형태로 응답한다")
    void should_return_200_with_api_response_when_valid_request() throws Exception {
        ImageGenerationResult result = new ImageGenerationResult(
                "/uploads/portraits/portrait-abc.jpg",
                "https://image.pollinations.ai/prompt/x",
                "a cute pink rabbit",
                "pollinations-flux",
                0.0);
        when(imageGenerationService.generate(eq("a cute pink rabbit"), any(), any(), anyString()))
                .thenReturn(result);

        PortraitGenerationRequest body = new PortraitGenerationRequest("a cute pink rabbit", null, null);

        mockMvc.perform(post("/api/images/portraits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.localPath").value("/uploads/portraits/portrait-abc.jpg"))
                .andExpect(jsonPath("$.data.externalUrl").value("https://image.pollinations.ai/prompt/x"))
                .andExpect(jsonPath("$.data.modelName").value("pollinations-flux"));
    }

    @Test
    @DisplayName("프롬프트가 비어 있으면 @Valid 가 400 으로 거절한다")
    void should_return_400_when_prompt_blank() throws Exception {
        PortraitGenerationRequest body = new PortraitGenerationRequest("", null, null);

        mockMvc.perform(post("/api/images/portraits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("일일 한도 초과는 429 + ApiResponse.fail(IMAGE_QUOTA_EXCEEDED) 로 응답한다")
    void should_return_429_when_quota_exceeded() throws Exception {
        when(imageGenerationService.generate(anyString(), any(), any(), anyString()))
                .thenThrow(new ImageException(ErrorCode.IMAGE_QUOTA_EXCEEDED));

        PortraitGenerationRequest body = new PortraitGenerationRequest("a cat", null, null);

        mockMvc.perform(post("/api/images/portraits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("I002"));
    }
}
