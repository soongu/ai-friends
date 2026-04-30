package kr.spartaclub.aifriends.chat.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Day 5 Step 4 — ChatClientConfig 가 ChatMemory 와 ChatClient 빈을 등록하는지,
 * ChatMemory 가 JdbcChatMemoryRepository 와 연결되어 add → get → clear 사이클이 도는지 검증한다.
 *
 * <p>ChatClient 의 default advisor 등록 자체는 내부 필드라 직접 검증이 어렵지만,
 * (a) ChatMemory 빈이 MessageWindowChatMemory 로 등록되었는지,
 * (b) 그 ChatMemory 가 같은 conversationId 로 add 한 메시지를 정확히 돌려주는지
 * 두 축으로 advisor 가 호출 시점에 의존하게 될 ChatMemory 가 멀쩡함을 보장한다.</p>
 */
@SpringBootTest(
        properties = {
                "DB_URL=jdbc:h2:mem:chat-memory-config-test;DB_CLOSE_DELAY=-1",
                "spring.ai.model.chat=openai",
                "spring.ai.openai.api-key=test-dummy"
        }
)
class ChatMemoryConfigIntegrationTest {

    @Autowired
    ChatMemory chatMemory;

    @Autowired
    ChatClient soulmateChatClient;

    @Test
    @DisplayName("ChatMemory 빈은 MessageWindowChatMemory(slide window) 로 등록된다")
    void chatMemory_isMessageWindowChatMemory() {
        assertThat(chatMemory).isInstanceOf(MessageWindowChatMemory.class);
    }

    @Test
    @DisplayName("ChatClient 빈은 default advisor + ChatMemory 와 함께 빌드되어 주입 가능하다")
    void soulmateChatClient_isAutowired() {
        assertThat(soulmateChatClient).isNotNull();
    }

    @Test
    @DisplayName("ChatMemory.add → get — 같은 conversationId 로 저장한 메시지가 순서대로 돌아온다")
    void chatMemory_addThenGet_returnsMessagesInOrder() {
        String conv = UUID.randomUUID().toString();
        chatMemory.add(conv, List.of(
                new UserMessage("오늘 좀 우울해"),
                new AssistantMessage("에이, 무슨 일 있었어?")
        ));

        var loaded = chatMemory.get(conv);

        assertThat(loaded).hasSize(2);
        assertThat(loaded.get(0).getMessageType()).isEqualTo(MessageType.USER);
        assertThat(loaded.get(1).getMessageType()).isEqualTo(MessageType.ASSISTANT);

        chatMemory.clear(conv);
    }

    @Test
    @DisplayName("ChatMemory.clear — 해당 세션만 비우고 다른 세션은 남는다")
    void chatMemory_clear_isolatedPerConversation() {
        String convA = UUID.randomUUID().toString();
        String convB = UUID.randomUUID().toString();
        chatMemory.add(convA, List.of(new UserMessage("a-1")));
        chatMemory.add(convB, List.of(new UserMessage("b-1")));

        chatMemory.clear(convA);

        assertThat(chatMemory.get(convA)).isEmpty();
        assertThat(chatMemory.get(convB)).hasSize(1);

        chatMemory.clear(convB);
    }
}
