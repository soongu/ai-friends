package kr.spartaclub.aifriends.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Day 3 Step 2 — 소꿉친구 페르소나 ChatClient 를 사용하는 서비스 단위 테스트.
 *
 * <p>주입받은 ChatClient 체인(.prompt().user().call().content()) 이 그대로 호출되고
 * 결과 문자열이 그대로 반환되는지만 검증한다. 페르소나(= defaultSystem) 자체는
 * ChatClientConfig 에서 빌더로 박혀 있어 이 서비스 계층의 책임이 아니다.</p>
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
    @DisplayName("chat() — 주입받은 ChatClient 로 user 메시지를 넘기고 응답 content 를 그대로 반환한다")
    void chat_delegatesUserMessageToChatClient() {
        given(chatClient.prompt().user(anyString()).call().content())
                .willReturn("에이, 무슨 일 있었어? 천천히 얘기해봐.");

        String reply = service.chat("오늘 진짜 별로였어");

        assertThat(reply).isEqualTo("에이, 무슨 일 있었어? 천천히 얘기해봐.");
        verify(chatClient.prompt()).user("오늘 진짜 별로였어");
    }
}
