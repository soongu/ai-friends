package kr.spartaclub.aifriends.chat.service;

import kr.spartaclub.aifriends.chat.dto.AiReply;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;

/**
 * Day 3 Step 3 — PromptTemplate 람다 문법으로 시스템 프롬프트를 조립하는 서비스 단위 테스트.
 * Day 4 Step 5 — 응답 타입을 String 에서 {@link AiReply} record 로 교체한 뒤 어설션도 record 형태로 갱신.
 *
 * <p>검증 포인트는 두 축이다.
 * <ol>
 *   <li>ChatClient 체인이 정상 동작해 .entity(AiReply.class) 가 돌려준 record 가 호출부로 흘러나온다.</li>
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
    @DisplayName("chat() — 주입받은 ChatClient 의 .entity(AiReply.class) 가 돌려준 record 를 그대로 반환한다")
    void chat_returnsChatClientEntity() {
        AiReply stub = new AiReply("에이, 무슨 일 있었어? 천천히 얘기해봐.",
                List.of("괜찮아", "잘 모르겠어", "조금 나아졌어"), 1);
        given(chatClient.prompt()
                .system(any(Consumer.class))
                .user(anyString())
                .advisors(any(Consumer.class))
                .call()
                .entity(eq(AiReply.class)))
                .willReturn(stub);

        AiReply reply = service.chat("conv-1", "user_1", "우울", "힘들어");

        assertThat(reply).isSameAs(stub);
        assertThat(reply.aiMessage()).isEqualTo("에이, 무슨 일 있었어? 천천히 얘기해봐.");
        assertThat(reply.choices()).hasSize(3);
        assertThat(reply.affectionDelta()).isEqualTo(1);
    }

    @Test
    @DisplayName("chat() — .system(Consumer) 에 템플릿과 익명 ID/mood 파라미터를 바인딩한다")
    void chat_bindsTemplateAndParamsIntoSystemSpec() {
        given(chatClient.prompt()
                .system(any(Consumer.class))
                .user(anyString())
                .advisors(any(Consumer.class))
                .call()
                .entity(eq(AiReply.class)))
                .willReturn(new AiReply("ok", List.of(), 0));
        // 스터빙 체인이 .system(any()) 을 한 번 호출하므로,
        // 실제 서비스 호출 전 interaction 카운터를 리셋해 then().should() 가 "진짜 람다" 만 붙잡도록 한다.
        clearInvocations(chatClient.prompt());

        service.chat("conv-1", "user_1", "우울", "힘들어");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<ChatClient.PromptSystemSpec>> captor =
                ArgumentCaptor.forClass(Consumer.class);
        then(chatClient.prompt()).should().system(captor.capture());

        ChatClient.PromptSystemSpec systemSpec =
                mock(ChatClient.PromptSystemSpec.class, Answers.RETURNS_SELF);
        captor.getValue().accept(systemSpec);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        then(systemSpec).should().text(textCaptor.capture());
        assertThat(textCaptor.getValue())
                .contains("{userName}")
                .contains("{mood}");

        then(systemSpec).should().param("userName", "user_1");
        then(systemSpec).should().param("mood", "우울");
    }
}
