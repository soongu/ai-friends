package kr.spartaclub.aifriends.chat.controller;

import kr.spartaclub.aifriends.chat.dto.AiReply;
import kr.spartaclub.aifriends.chat.privacy.UserAnonymizer;
import kr.spartaclub.aifriends.chat.service.SoulmateChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Day 3 Step 3 — SoulmateChatController 슬라이스 테스트.
 * Day 4 Step 5 — 응답 타입이 평문 String → {@link AiReply} record 로 바뀌면서 JSON 응답으로 검증.
 *
 * <p>UserAnonymizer 는 값 변환 하나만 하므로 실제 빈을 @Import 해서 엔드-투-엔드로 검증한다.
 * ChatClient 가 관여하는 페르소나 로직은 서비스 단위 테스트의 책임이다.</p>
 *
 * <p>Day 5 Step 5 — conversationId 파라미터 분리 + 세션 조회/삭제 엔드포인트 추가에 맞춰
 * 응답 jsonPath 가 {@code $.data.conversationId} / {@code $.data.reply.aiMessage} 로 갱신된다.</p>
 */
@WebMvcTest(SoulmateChatController.class)
@Import(UserAnonymizer.class)
class SoulmateChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SoulmateChatService service;

    @MockBean
    private ChatMemory chatMemory;

    @Test
    @DisplayName("GET /api/chat/soulmate - conversationId 누락 시 서버가 새 UUID 를 발급해 응답에 함께 내려준다")
    void soulmate_generatesNewConversationId_whenMissing() throws Exception {
        given(service.chat(anyString(), anyString(), anyString(), anyString()))
                .willReturn(new AiReply("에이, 무슨 일 있었어? 천천히 얘기해봐.",
                        List.of("괜찮아", "잘 모르겠어", "조금 나아졌어"), 1));

        mockMvc.perform(get("/api/chat/soulmate")
                        .param("userId", "1")
                        .param("mood", "우울")
                        .param("message", "오늘 진짜 별로였어"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.conversationId").isNotEmpty())
                .andExpect(jsonPath("$.data.reply.aiMessage").value("에이, 무슨 일 있었어? 천천히 얘기해봐."))
                .andExpect(jsonPath("$.data.reply.choices.length()").value(3))
                .andExpect(jsonPath("$.data.reply.affectionDelta").value(1));
    }

    @Test
    @DisplayName("GET /api/chat/soulmate - conversationId 가 주어지면 그 값을 그대로 서비스와 응답에 흘려보낸다")
    void soulmate_passesThroughExistingConversationId() throws Exception {
        String existingConv = "11111111-2222-3333-4444-555555555555";
        given(service.chat(eq(existingConv), eq("user_1"), eq("우울"), eq("두 번째 메시지야")))
                .willReturn(new AiReply("응, 아까 우울하다고 했지", List.of("응"), 0));

        mockMvc.perform(get("/api/chat/soulmate")
                        .param("userId", "1")
                        .param("mood", "우울")
                        .param("message", "두 번째 메시지야")
                        .param("conversationId", existingConv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value(existingConv))
                .andExpect(jsonPath("$.data.reply.aiMessage").value("응, 아까 우울하다고 했지"));

        then(service).should().chat(existingConv, "user_1", "우울", "두 번째 메시지야");
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
        then(service).should(never()).chat(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("GET /api/chat/soulmate/sessions/{conversationId} - 저장된 메시지를 role/content 뷰로 내려준다")
    void getSession_returnsMessageViews() throws Exception {
        String conv = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
        given(chatMemory.get(conv)).willReturn(List.of(
                new UserMessage("오늘 좀 우울해"),
                new AssistantMessage("에이, 무슨 일 있었어?")
        ));

        mockMvc.perform(get("/api/chat/soulmate/sessions/" + conv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].role").value("user"))
                .andExpect(jsonPath("$.data[0].content").value("오늘 좀 우울해"))
                .andExpect(jsonPath("$.data[1].role").value("assistant"))
                .andExpect(jsonPath("$.data[1].content").value("에이, 무슨 일 있었어?"));
    }

    @Test
    @DisplayName("DELETE /api/chat/soulmate/sessions/{conversationId} - ChatMemory.clear 를 호출하고 200 을 응답한다")
    void deleteSession_clearsChatMemory() throws Exception {
        String conv = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

        mockMvc.perform(delete("/api/chat/soulmate/sessions/" + conv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(chatMemory).should().clear(conv);
    }
}
