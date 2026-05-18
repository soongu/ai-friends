package kr.spartaclub.aifriends.tool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.spartaclub.aifriends.tool.dto.AffinityChatRequest;
import kr.spartaclub.aifriends.tool.dto.AffinityChatResponse;
import kr.spartaclub.aifriends.tool.service.AffinityChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Day 11 Step 4 — {@link AffinityChatController} 슬라이스 테스트.
 *
 * <p>Service 를 {@code @MockBean} 으로 stub 처리해 컨트롤러의 입출력 계약(ApiResponse 래핑 + 검증) 만 본다.
 * ChatClient 동작 · 실제 도구 디스패치는 강사 수동 smoke 의 책임이다 (Step 2 ·3 와 동일한 결).</p>
 */
@WebMvcTest(AffinityChatController.class)
class AffinityChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AffinityChatService service;

    @Test
    @DisplayName("POST /api/tool/affinity/chat - 정상 요청이면 ApiResponse.success 와 캐릭터 응답을 내려준다")
    void chat_returnsCharacterReply() throws Exception {
        given(service.chat(eq(7L), anyString())).willReturn(new AffinityChatResponse(
                7L,
                "음… 우리 단짝 정도? 너랑 얘기할 때마다 점점 가까워지는 거 같아."));

        AffinityChatRequest request = new AffinityChatRequest(7L, "지금 우리 사이 어때?");

        mockMvc.perform(post("/api/tool/affinity/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.soulmateId").value(7))
                .andExpect(jsonPath("$.data.aiMessage").value(
                        "음… 우리 단짝 정도? 너랑 얘기할 때마다 점점 가까워지는 거 같아."));

        then(service).should().chat(7L, "지금 우리 사이 어때?");
    }

    @Test
    @DisplayName("POST /api/tool/affinity/chat - soulmateId 가 null 이면 400 + ApiResponse.fail 을 반환한다")
    void chat_nullSoulmateId_returns400() throws Exception {
        AffinityChatRequest request = new AffinityChatRequest(null, "안녕");

        mockMvc.perform(post("/api/tool/affinity/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("E001"));

        then(service).should(never()).chat(anyLong(), anyString());
    }

    @Test
    @DisplayName("POST /api/tool/affinity/chat - message 가 빈 문자열이면 400 + ApiResponse.fail 을 반환한다")
    void chat_blankMessage_returns400() throws Exception {
        AffinityChatRequest request = new AffinityChatRequest(7L, "");

        mockMvc.perform(post("/api/tool/affinity/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("E001"));

        then(service).should(never()).chat(anyLong(), anyString());
    }
}
