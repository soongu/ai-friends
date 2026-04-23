package kr.spartaclub.aifriends.chat.controller;

import kr.spartaclub.aifriends.chat.privacy.UserAnonymizer;
import kr.spartaclub.aifriends.chat.service.SoulmateChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Day 3 Step 3 — SoulmateChatController 슬라이스 테스트.
 *
 * <p>UserAnonymizer 는 값 변환 하나만 하므로 실제 빈을 @Import 해서 엔드-투-엔드로 검증한다.
 * ChatClient 가 관여하는 페르소나 로직은 서비스 단위 테스트의 책임이다.</p>
 */
@WebMvcTest(SoulmateChatController.class)
@Import(UserAnonymizer.class)
class SoulmateChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SoulmateChatService service;

    @Test
    @DisplayName("GET /api/chat/soulmate - userId 를 user_{id} 로 익명화해 서비스에 넘긴다")
    void soulmate_anonymizesUserIdBeforeDelegation() throws Exception {
        given(service.chat(anyString(), anyString(), anyString()))
                .willReturn("에이, 무슨 일 있었어? 천천히 얘기해봐.");

        mockMvc.perform(get("/api/chat/soulmate")
                        .param("userId", "1")
                        .param("mood", "우울")
                        .param("message", "오늘 진짜 별로였어"))
                .andExpect(status().isOk())
                .andExpect(content().string("에이, 무슨 일 있었어? 천천히 얘기해봐."));

        verify(service).chat("user_1", "우울", "오늘 진짜 별로였어");
    }
}
