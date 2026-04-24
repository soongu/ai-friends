package kr.spartaclub.aifriends.hello;

import kr.spartaclub.aifriends.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Day 2 과제 1 — BenchmarkController 슬라이스 테스트.
 *
 * <p>HelloAiControllerTest 와 동일하게 ChatClient.Builder 는 TestConfiguration 에서
 * 고정된 ChatClient(deep stub) 를 반환하도록 구성한다. ChatClient 호출은 mock 이라
 * latency 자체는 거의 0 에 가깝지만, 통계 필드의 형태와 호출 횟수 검증은 충분히 가능하다.</p>
 */
@WebMvcTest(BenchmarkController.class)
@Import({BenchmarkControllerTest.ChatClientTestConfig.class, GlobalExceptionHandler.class})
class BenchmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatClient chatClient;

    @MockBean
    private ProviderInfo providerInfo;

    @Test
    @DisplayName("GET /api/benchmark - iterations 만큼 호출하여 통계와 sampleReply 를 담은 JSON 을 반환한다")
    void benchmark_returnsLatencyStats() throws Exception {
        reset(chatClient);
        given(providerInfo.currentLabel()).willReturn("gemini-2.5-flash-lite");
        given(chatClient.prompt().user(anyString()).call().content())
                .willReturn("안녕하세요! 저는 AI 어시스턴트입니다.");

        mockMvc.perform(get("/api/benchmark")
                        .param("message", "자기소개")
                        .param("iterations", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("gemini-2.5-flash-lite"))
                .andExpect(jsonPath("$.message").value("자기소개"))
                .andExpect(jsonPath("$.iterations").value(5))
                .andExpect(jsonPath("$.latencyStats.minMs").isNumber())
                .andExpect(jsonPath("$.latencyStats.maxMs").isNumber())
                .andExpect(jsonPath("$.latencyStats.avgMs").isNumber())
                .andExpect(jsonPath("$.latencyStats.allMs").isArray())
                .andExpect(jsonPath("$.latencyStats.allMs.length()").value(5))
                .andExpect(jsonPath("$.sampleReply").value("안녕하세요! 저는 AI 어시스턴트입니다."));
    }

    @Test
    @DisplayName("GET /api/benchmark - iterations 파라미터 없으면 기본값 3 으로 동작한다")
    void benchmark_noIterations_usesDefault3() throws Exception {
        reset(chatClient);
        given(providerInfo.currentLabel()).willReturn("ollama-gemma3:4b");
        given(chatClient.prompt().user(anyString()).call().content())
                .willReturn("hi");

        mockMvc.perform(get("/api/benchmark").param("message", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iterations").value(3))
                .andExpect(jsonPath("$.latencyStats.allMs.length()").value(3));
    }

    @Test
    @DisplayName("GET /api/benchmark - iterations 가 0 이하면 400 Bad Request 를 반환한다")
    void benchmark_iterationsTooLow_returns400() throws Exception {
        mockMvc.perform(get("/api/benchmark")
                        .param("message", "test")
                        .param("iterations", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/benchmark - iterations 가 10 을 넘으면 400 Bad Request 를 반환한다")
    void benchmark_iterationsTooHigh_returns400() throws Exception {
        mockMvc.perform(get("/api/benchmark")
                        .param("message", "test")
                        .param("iterations", "11"))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class ChatClientTestConfig {

        @Bean
        ChatClient chatClient() {
            return mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        }

        @Bean
        ChatClient.Builder chatClientBuilder(ChatClient chatClient) {
            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            given(builder.build()).willReturn(chatClient);
            return builder;
        }
    }
}
