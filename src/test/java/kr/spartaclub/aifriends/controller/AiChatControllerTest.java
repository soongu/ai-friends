package kr.spartaclub.aifriends.controller;

import kr.spartaclub.aifriends.common.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;
import kr.spartaclub.aifriends.dto.AiChatRequest;
import kr.spartaclub.aifriends.dto.AiChatResponse;
import kr.spartaclub.aifriends.service.AiChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiChatController.class)
@Import(GlobalExceptionHandler.class)
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiChatService aiChatService;

    @Test
    @DisplayName("AI 채팅 요청 성공")
    void chatWithAi_success() throws Exception {
        // given
        AiChatRequest request = new AiChatRequest(1L, "안녕");
        AiChatResponse response = new AiChatResponse(
                "안녕", "반가워!", Collections.emptyList(), 1L, 5, 1, Collections.emptyList()
        );

        given(aiChatService.processChat(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userMessage").value("안녕"))
                .andExpect(jsonPath("$.data.aiMessage").value("반가워!"))
                .andExpect(jsonPath("$.data.affectionScore").value(5));
    }

    @Test
    @DisplayName("AI 채팅 요청 유효성 검사 실패 (메시지 누락)")
    void chatWithAi_validationFail() throws Exception {
        // given
        AiChatRequest request = new AiChatRequest(1L, "");

        // when & then
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("메시지를 입력해 주세요."));
    }
}
