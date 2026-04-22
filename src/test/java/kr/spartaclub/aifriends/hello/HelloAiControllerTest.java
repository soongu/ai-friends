package kr.spartaclub.aifriends.hello;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HelloAiController 슬라이스 테스트.
 *
 * <p>ChatClient.Builder 는 TestConfiguration 에서 고정된 ChatClient(deep stub) 를
 * 반환하도록 구성하여, 컨트롤러가 생성 시 보관한 인스턴스와 테스트가 stub 하는
 * 인스턴스가 동일한지 보장한다. 테스트마다 reset() 으로 체인 stub 을 초기화한다.</p>
 */
@WebMvcTest(HelloAiController.class)
@Import(HelloAiControllerTest.ChatClientTestConfig.class)
class HelloAiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatClient chatClient;

    @Test
    @DisplayName("GET /api/hello-ai - message 파라미터를 그대로 user 메시지로 넘겨 ChatClient 응답을 반환한다")
    void hello_withMessageParam_returnsChatClientContent() throws Exception {
        reset(chatClient);
        given(chatClient.prompt().user(anyString()).call().content())
                .willReturn("저는 Day 1 Hello AI 입니다.");

        mockMvc.perform(get("/api/hello-ai").param("message", "자기소개 해줘"))
                .andExpect(status().isOk())
                .andExpect(content().string("저는 Day 1 Hello AI 입니다."));
    }

    @Test
    @DisplayName("GET /api/hello-ai - message 파라미터 없이도 기본 메시지로 200 응답한다")
    void hello_withoutMessageParam_usesDefault() throws Exception {
        reset(chatClient);
        given(chatClient.prompt().user(anyString()).call().content())
                .willReturn("default-ok");

        mockMvc.perform(get("/api/hello-ai"))
                .andExpect(status().isOk())
                .andExpect(content().string("default-ok"));
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
