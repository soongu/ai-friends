package kr.spartaclub.aifriends.chat.controller;

import kr.spartaclub.aifriends.chat.dto.AiReply;
import kr.spartaclub.aifriends.chat.privacy.UserAnonymizer;
import kr.spartaclub.aifriends.chat.service.SoulmateChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Day 3 Step 3 — SoulmateChatController 슬라이스 테스트.
 * Day 4 Step 5 — 응답 타입이 평문 String → {@link AiReply} record 로 바뀌면서 JSON 응답으로 검증.
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
    @DisplayName("GET /api/chat/soulmate - userId 를 user_{id} 로 익명화해 서비스에 넘기고 AiReply 를 JSON 으로 응답한다")
    void soulmate_anonymizesUserIdBeforeDelegation() throws Exception {
        given(service.chat(anyString(), anyString(), anyString()))
                .willReturn(new AiReply("에이, 무슨 일 있었어? 천천히 얘기해봐.",
                        List.of("괜찮아", "잘 모르겠어", "조금 나아졌어"), 1));

        mockMvc.perform(get("/api/chat/soulmate")
                        .param("userId", "1")
                        .param("mood", "우울")
                        .param("message", "오늘 진짜 별로였어"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.aiMessage").value("에이, 무슨 일 있었어? 천천히 얘기해봐."))
                .andExpect(jsonPath("$.data.choices.length()").value(3))
                .andExpect(jsonPath("$.data.choices[0]").value("괜찮아"))
                .andExpect(jsonPath("$.data.affectionDelta").value(1));

        then(service).should().chat("user_1", "우울", "오늘 진짜 별로였어");
    }

    @Test
    @DisplayName("GET /api/chat/soulmate - 필수 파라미터 누락 시 400 과 BAD_REQUEST 에러 응답을 반환한다")
    void soulmate_missingRequiredParam_returns400() throws Exception {
        mockMvc.perform(get("/api/chat/soulmate")
                        .param("userId", "1")
                        .param("message", "안녕"))   // mood 누락
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(400))
                .andExpect(jsonPath("$.error.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.code").value("E001"))
                .andExpect(jsonPath("$.error.message").value(
                        org.hamcrest.Matchers.containsString("mood")));
    }
}
