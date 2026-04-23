package kr.spartaclub.aifriends.hello;

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

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    @MockBean
    private ProviderInfo providerInfo;

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

    @Test
    @DisplayName("GET /api/hello-ai/v2 - provider / message / reply / latencyMs 4필드를 담은 JSON 응답을 반환한다")
    void helloV2_returnsJsonWithProviderAndLatency() throws Exception {
        reset(chatClient);
        given(providerInfo.currentLabel()).willReturn("ollama-llama3.2:3b");
        given(chatClient.prompt().user(anyString()).call().content())
                .willReturn("Docker / RDB / Kafka 3가지 추천드려요.");

        mockMvc.perform(get("/api/hello-ai/v2").param("message", "기술 3가지 추천해줘"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("ollama-llama3.2:3b"))
                .andExpect(jsonPath("$.message").value("기술 3가지 추천해줘"))
                .andExpect(jsonPath("$.reply").value("Docker / RDB / Kafka 3가지 추천드려요."))
                .andExpect(jsonPath("$.latencyMs").isNumber());
    }

    @Test
    @DisplayName("GET /api/hello-ai/v2 - message 파라미터 없이도 기본 메시지로 200 JSON 응답을 반환한다")
    void helloV2_withoutMessageParam_usesDefault() throws Exception {
        reset(chatClient);
        given(providerInfo.currentLabel()).willReturn("gemini-2.5-flash-lite");
        given(chatClient.prompt().user(anyString()).call().content())
                .willReturn("안녕하세요, Gemini 입니다.");

        mockMvc.perform(get("/api/hello-ai/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("gemini-2.5-flash-lite"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.reply").value("안녕하세요, Gemini 입니다."));
    }

    @Test
    @DisplayName("GET /api/hello-ai/v3 - tutor-v1.st 의 {userName}·{topicTag} 슬롯이 치환되어 .system() 에 주입된다")
    void helloV3_bindsTopicTagAndAnonymizedIdIntoSystemPrompt() throws Exception {
        reset(chatClient);
        given(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .willReturn("좋은 질문이야! 의존성 주입은 ... 그럼 다음 질문?");
        // 스터빙 체인이 .system(anyString()) 을 한 번 호출하므로 실제 요청 전에 invocation 을 초기화해
        // then().should() 가 "진짜 호출" 만 붙잡도록 한다.
        clearInvocations(chatClient.prompt());

        mockMvc.perform(get("/api/hello-ai/v3")
                        .param("message", "의존성 주입이 뭔가요?")
                        .param("topicTag", "Java"))
                .andExpect(status().isOk())
                .andExpect(content().string("좋은 질문이야! 의존성 주입은 ... 그럼 다음 질문?"));

        ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
        then(chatClient.prompt()).should().system(systemCaptor.capture());
        assertThat(systemCaptor.getValue())
                .contains("# Role")
                .contains("# Context")
                .contains("# Task")
                .contains("Java")                      // topicTag 치환 확인
                .contains("tutor-student-1")           // 익명 ID 치환 확인 (시스템 프롬프트 내부)
                .doesNotContain("{userName}", "{topicTag}");   // 플레이스홀더 잔재 없음

        then(chatClient.prompt().system(anyString())).should().user("의존성 주입이 뭔가요?");
    }

    @Test
    @DisplayName("GET /api/hello-ai/v3 - message·topicTag 기본값(의존성 주입/Spring AI)으로 200 응답한다")
    void helloV3_usesDefaultParams() throws Exception {
        reset(chatClient);
        given(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .willReturn("default-v3-ok");
        clearInvocations(chatClient.prompt());

        mockMvc.perform(get("/api/hello-ai/v3"))
                .andExpect(status().isOk())
                .andExpect(content().string("default-v3-ok"));

        ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
        then(chatClient.prompt()).should().system(systemCaptor.capture());
        assertThat(systemCaptor.getValue()).contains("Spring AI");   // topicTag 기본값 치환

        then(chatClient.prompt().system(anyString())).should().user("의존성 주입이 뭔가요?");
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
