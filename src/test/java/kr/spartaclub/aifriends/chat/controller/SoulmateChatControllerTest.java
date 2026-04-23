package kr.spartaclub.aifriends.chat.controller;

import kr.spartaclub.aifriends.chat.service.SoulmateChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Day 3 Step 2 — SoulmateChatController 슬라이스 테스트.
 *
 * <p>컨트롤러는 얇은 위임 계층이므로 Service 만 MockBean 으로 대체해
 * 라우팅·파라미터 바인딩·응답 매핑만 검증한다. ChatClient 자체는 서비스 단위 테스트에서 본다.</p>
 */
@WebMvcTest(SoulmateChatController.class)
class SoulmateChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SoulmateChatService service;

    @Test
    @DisplayName("GET /api/chat/soulmate - message 파라미터를 서비스에 위임하고 응답을 그대로 반환한다")
    void soulmate_delegatesToService() throws Exception {
        given(service.chat(anyString()))
                .willReturn("에이, 무슨 일 있었어? 천천히 얘기해봐.");

        mockMvc.perform(get("/api/chat/soulmate").param("message", "오늘 진짜 별로였어"))
                .andExpect(status().isOk())
                .andExpect(content().string("에이, 무슨 일 있었어? 천천히 얘기해봐."));

        verify(service).chat("오늘 진짜 별로였어");
    }
}
