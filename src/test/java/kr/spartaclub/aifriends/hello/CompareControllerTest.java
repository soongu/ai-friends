package kr.spartaclub.aifriends.hello;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Day 2 과제 2 — CompareController 슬라이스 테스트.
 *
 * <p>compare 프로파일을 실제로 활성화하지 않고, 두 개의 {@link ChatModel} 을
 * {@code @Qualifier} 이름("ollamaChatModel" / "geminiChatModel") 으로 직접 등록해
 * 컨트롤러가 양쪽을 병렬 호출하고 결과를 한 응답에 합쳐 반환하는지 검증한다.</p>
 *
 * <p>라벨 조립에 쓰이는 {@code app.compare.*.model} 프로퍼티는
 * {@link TestPropertySource} 로 고정한다.</p>
 */
@WebMvcTest(CompareController.class)
@Import(CompareControllerTest.TestChatModelConfig.class)
@ActiveProfiles("compare")
@TestPropertySource(properties = {
        "app.compare.ollama.model=gemma3:4b",
        "app.compare.gemini.model=gemini-2.5-flash-lite"
})
class CompareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("ollamaChatModel")
    private ChatModel ollamaChatModel;

    @Autowired
    @Qualifier("geminiChatModel")
    private ChatModel geminiChatModel;

    @Test
    @DisplayName("GET /api/compare - 두 프로바이더 응답을 results 배열에 순서대로 담아 반환한다")
    void compare_returnsBothResults() throws Exception {
        reset(ollamaChatModel, geminiChatModel);
        given(ollamaChatModel.call(any(Prompt.class))).willReturn(chatResponse("안녕 ollama"));
        given(geminiChatModel.call(any(Prompt.class))).willReturn(chatResponse("안녕 gemini"));

        mockMvc.perform(get("/api/compare").param("message", "자기소개"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("자기소개"))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].provider").value("ollama-gemma3:4b"))
                .andExpect(jsonPath("$.results[0].reply").value("안녕 ollama"))
                .andExpect(jsonPath("$.results[0].latencyMs").isNumber())
                .andExpect(jsonPath("$.results[1].provider").value("gemini-2.5-flash-lite"))
                .andExpect(jsonPath("$.results[1].reply").value("안녕 gemini"))
                .andExpect(jsonPath("$.results[1].latencyMs").isNumber());
    }

    @Test
    @DisplayName("GET /api/compare - 한 프로바이더가 예외를 던져도 다른 프로바이더 응답은 그대로 반환한다")
    void compare_whenOneFails_otherStillReturns() throws Exception {
        reset(ollamaChatModel, geminiChatModel);
        given(ollamaChatModel.call(any(Prompt.class)))
                .willThrow(new RuntimeException("connection refused"));
        given(geminiChatModel.call(any(Prompt.class))).willReturn(chatResponse("gemini ok"));

        mockMvc.perform(get("/api/compare").param("message", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].provider").value("ollama-gemma3:4b"))
                .andExpect(jsonPath("$.results[0].reply").value(startsWith("ERROR:")))
                .andExpect(jsonPath("$.results[1].provider").value("gemini-2.5-flash-lite"))
                .andExpect(jsonPath("$.results[1].reply").value("gemini ok"));
    }

    private static ChatResponse chatResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @TestConfiguration
    static class TestChatModelConfig {

        @Bean("ollamaChatModel")
        ChatModel ollamaChatModel() {
            return mock(ChatModel.class);
        }

        @Bean("geminiChatModel")
        ChatModel geminiChatModel() {
            return mock(ChatModel.class);
        }
    }
}
