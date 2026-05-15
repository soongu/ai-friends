package kr.spartaclub.aifriends.chat.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Day 5 Step 3 — JdbcChatMemoryRepository 가 starter 자동설정으로 빈 등록되고
 * H2 인메모리 DB 에 SPRING_AI_CHAT_MEMORY 테이블을 자동 초기화한 뒤
 * 메시지 저장/조회/삭제 사이클이 도는지 검증한다.
 *
 * <p>스키마 초기화는 application.yml 의
 * {@code spring.ai.chat.memory.repository.jdbc.initialize-schema=always} 가 책임진다.</p>
 */
@SpringBootTest(
        properties = {
                "DB_URL=jdbc:h2:mem:chat-memory-test;DB_CLOSE_DELAY=-1",
                // ChatClient.Builder 자동설정이 ChatModel 빈을 요구하므로 openai 스타터만 활성화
                "spring.ai.model.chat=openai",
                "spring.ai.openai.api-key=test-dummy"
        }
)
class JdbcChatMemoryRepositoryIntegrationTest {

    @Autowired
    ChatMemoryRepository repository;

    @Test
    @DisplayName("saveAll → findByConversationId — 저장한 메시지가 동일 순서·타입으로 조회된다")
    void saveAll_then_findByConversationId_returnsMessagesInOrder() {
        // SPRING_AI_CHAT_MEMORY.conversation_id 는 VARCHAR(36) — UUID 포맷에 맞춘다
        String conv = UUID.randomUUID().toString();
        List<Message> messages = List.of(
                new UserMessage("안녕, 나 오늘 좀 우울해"),
                new AssistantMessage("에이, 무슨 일 있어? 천천히 얘기해줘.")
        );

        repository.saveAll(conv, messages);
        List<Message> loaded = repository.findByConversationId(conv);

        assertThat(loaded).hasSize(2);
        assertThat(loaded.get(0).getMessageType()).isEqualTo(MessageType.USER);
        assertThat(loaded.get(0).getText()).isEqualTo("안녕, 나 오늘 좀 우울해");
        assertThat(loaded.get(1).getMessageType()).isEqualTo(MessageType.ASSISTANT);
        assertThat(loaded.get(1).getText()).isEqualTo("에이, 무슨 일 있어? 천천히 얘기해줘.");

        repository.deleteByConversationId(conv);
    }

    @Test
    @DisplayName("deleteByConversationId — 해당 conversationId 의 메시지가 모두 사라지고 다른 세션은 보존된다")
    void deleteByConversationId_clearsOnlyTargetConversation() {
        String convA = UUID.randomUUID().toString();
        String convB = UUID.randomUUID().toString();
        repository.saveAll(convA, List.of(new UserMessage("a-1")));
        repository.saveAll(convB, List.of(new UserMessage("b-1")));

        repository.deleteByConversationId(convA);

        assertThat(repository.findByConversationId(convA)).isEmpty();
        assertThat(repository.findByConversationId(convB)).hasSize(1);

        repository.deleteByConversationId(convB);
    }

    @Test
    @DisplayName("findConversationIds — 저장된 세션 ID 목록을 돌려준다")
    void findConversationIds_listsSavedConversations() {
        String conv = UUID.randomUUID().toString();
        repository.saveAll(conv, List.of(new UserMessage("hi")));

        List<String> ids = repository.findConversationIds();

        assertThat(ids).contains(conv);

        repository.deleteByConversationId(conv);
    }
}
