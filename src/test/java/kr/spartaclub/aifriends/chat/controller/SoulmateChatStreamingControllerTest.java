package kr.spartaclub.aifriends.chat.controller;

import kr.spartaclub.aifriends.chat.privacy.UserAnonymizer;
import kr.spartaclub.aifriends.chat.service.SoulmateChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Day 6 Step 2~3 — 스트리밍 채팅 컨트롤러 슬라이스 테스트.
 *
 * <p>Spring MVC + Reactor 의 {@code ReactiveTypeHandler} 가 컨트롤러가 반환한 {@code Flux<String>}
 * 을 SSE 로 자동 변환하는지 검증한다. {@code MockMvc} 의 비동기 디스패치 패턴
 * ({@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders#asyncDispatch})
 * 으로 SSE 응답이 토큰 단위로 흘러나오는 것까지 확인한다.</p>
 *
 * <p>Day 6 Step 5 에서 ChatMemory 통합이 들어오면 {@code conversationId} 파라미터가 추가되지만,
 * 이번 턴에서는 가장 단순한 시그니처로 *토큰 스트리밍 자체* 가 흐르는지에만 집중한다.</p>
 */
@WebMvcTest(SoulmateChatController.class)
@Import(UserAnonymizer.class)
class SoulmateChatStreamingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SoulmateChatService service;

    @MockBean
    private ChatMemory chatMemory;

    @Test
    @DisplayName("GET /api/chat/soulmate/stream - Flux<String> 토큰들이 text/event-stream 으로 흘러나온다")
    void streamChat_streamsTokensAsServerSentEvents() throws Exception {
        given(service.chatStream(anyString(), eq("user_1"), eq("우울"), eq("힘들어")))
                .willReturn(Flux.just("오늘", " 많이", " 힘들었구나"));

        MvcResult mvcResult = mockMvc.perform(get("/api/chat/soulmate/stream")
                        .param("userId", "1")
                        .param("mood", "우울")
                        .param("message", "힘들어")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));

        // MockHttpServletResponse 의 기본 인코딩이 ISO-8859-1 이라 한글이 깨진 채로 잡힌다.
        // 컨트롤러가 흘려보낸 바이트를 직접 UTF-8 로 디코딩해 SSE 본문을 검증한다.
        String body = new String(
                mvcResult.getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);

        // SSE 포맷("data:" 프리픽스) 안에 토큰 3 조각이 모두 포함되어야 한다.
        assertThat(body)
                .contains("오늘")
                .contains("많이")
                .contains("힘들었구나");
    }

    @Test
    @DisplayName("GET /api/chat/soulmate/stream - userId 가 익명화된 별칭으로 서비스에 흘러간다")
    void streamChat_passesAnonymizedNameToService() throws Exception {
        given(service.chatStream(anyString(), anyString(), anyString(), anyString()))
                .willReturn(Flux.just("ok"));

        MvcResult mvcResult = mockMvc.perform(get("/api/chat/soulmate/stream")
                        .param("userId", "42")
                        .param("mood", "신남")
                        .param("message", "오늘 좋은 일 있었어")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk());

        // Day 6 Step 5 — conversationId 가 비어 있으면 서버가 새로 발급(blocking 엔드포인트와 동일한 정책).
        // 첫 번째 인자(conversationId) 는 UUID, 두 번째 인자부터 익명화된 user_42 / mood / message 가 흘러간다.
        then(service).should().chatStream(anyString(), eq("user_42"), eq("신남"), eq("오늘 좋은 일 있었어"));
    }

    @Test
    @DisplayName("GET /api/chat/soulmate/stream - 클라이언트가 conversationId 를 주면 그 값을 그대로 서비스로 흘려보낸다")
    void streamChat_passesThroughExistingConversationId() throws Exception {
        String existingConv = "11111111-2222-3333-4444-555555555555";
        given(service.chatStream(eq(existingConv), eq("user_1"), eq("우울"), eq("힘들어")))
                .willReturn(Flux.just("ok"));

        MvcResult mvcResult = mockMvc.perform(get("/api/chat/soulmate/stream")
                        .param("userId", "1")
                        .param("mood", "우울")
                        .param("message", "힘들어")
                        .param("conversationId", existingConv)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk());

        then(service).should().chatStream(existingConv, "user_1", "우울", "힘들어");
    }
}
