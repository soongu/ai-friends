package kr.spartaclub.aifriends.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Day 3 Step 3 — PromptTemplate 람다 문법으로 시스템 프롬프트를 조립하는 서비스 단위 테스트.
 *
 * <p>검증 포인트는 두 축이다.
 * <ol>
 *   <li>ChatClient 체인이 정상 동작해 응답 content 가 호출부로 흘러나온다.</li>
 *   <li>.system(Consumer) 에 넘긴 람다가 PromptSystemSpec.text() 에 {userName}·{mood} 슬롯을 가진
 *       템플릿을 넘기고, .param() 에 익명 ID 와 mood 를 각각 바인딩한다.</li>
 * </ol></p>
 */
class SoulmateChatServiceTest {

    private ChatClient chatClient;
    private SoulmateChatService service;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        service = new SoulmateChatService(chatClient);
    }

    @Test
    @DisplayName("chat() — 주입받은 ChatClient 로 응답 content 를 그대로 반환한다")
    void chat_returnsChatClientContent() {
        given(chatClient.prompt()
                .system(any(Consumer.class))
                .user(anyString())
                .call()
                .content())
                .willReturn("에이, 무슨 일 있었어? 천천히 얘기해봐.");

        String reply = service.chat("user_1", "우울", "힘들어");

        assertThat(reply).isEqualTo("에이, 무슨 일 있었어? 천천히 얘기해봐.");
    }

    @Test
    @DisplayName("chat() — .system(Consumer) 에 템플릿과 익명 ID/mood 파라미터를 바인딩한다")
    void chat_bindsTemplateAndParamsIntoSystemSpec() {
        given(chatClient.prompt()
                .system(any(Consumer.class))
                .user(anyString())
                .call()
                .content())
                .willReturn("ok");
        // 스터빙 체인이 .system(any()) 을 한 번 호출하므로,
        // 실제 서비스 호출 전 interaction 카운터를 리셋해 verify 가 "진짜 람다" 만 붙잡도록 한다.
        clearInvocations(chatClient.prompt());

        service.chat("user_1", "우울", "힘들어");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<ChatClient.PromptSystemSpec>> captor =
                ArgumentCaptor.forClass(Consumer.class);
        verify(chatClient.prompt()).system(captor.capture());

        ChatClient.PromptSystemSpec systemSpec =
                mock(ChatClient.PromptSystemSpec.class, Answers.RETURNS_SELF);
        captor.getValue().accept(systemSpec);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(systemSpec).text(textCaptor.capture());
        assertThat(textCaptor.getValue())
                .contains("{userName}")
                .contains("{mood}");

        verify(systemSpec).param("userName", "user_1");
        verify(systemSpec).param("mood", "우울");
    }
}
