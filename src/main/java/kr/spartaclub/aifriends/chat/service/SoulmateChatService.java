package kr.spartaclub.aifriends.chat.service;

import kr.spartaclub.aifriends.chat.dto.AiReply;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

/**
 * Day 3 Step 3 — 소꿉친구 페르소나 ChatClient 를 사용하는 서비스.
 *
 * <p>Step 2 에서는 ChatClientConfig 의 defaultSystem 이 페르소나의 전부였지만,
 * Step 3 부터는 호출 시점에 {userName}·{mood} 같은 동적 값을 꽂을 수 있도록
 * .system(Consumer) 람다 + PromptSystemSpec.text/param 으로 덮어쓴다.</p>
 *
 * <p>익명 ID 변환은 UserAnonymizer 가 책임지고, 이 서비스는 이미 익명화된 값만 받는다.
 * PII 가 흘러 들어오지 않도록 호출 계층에서 마스킹을 끝내고 넘기는 규율이다.</p>
 *
 * <p>Day 4 Step 5 — 응답 타입을 {@code String} 에서 {@link AiReply} record 로 교체했다.
 * {@code .call().content()} 대신 {@code .call().entity(AiReply.class)} 를 쓰면
 * BeanOutputConverter 가 record 의 JSON Schema 를 자동 생성해 프롬프트에 주입하고,
 * LLM 응답을 ObjectMapper 로 역직렬화까지 마쳐서 record 인스턴스를 돌려준다.</p>
 *
 * <p>Day 5 Step 5 — chat 시그니처에 {@code conversationId} 를 추가했다.
 * ChatClientConfig 에 등록된 {@code MessageChatMemoryAdvisor} 가 호출 직전·직후에
 * {@code ChatMemory.get/add} 를 자동으로 돌려주지만, *어느 세션* 의 이력인지는
 * advisor 파라미터로 conversationId 를 명시해야 사용자별 격리가 보장된다.</p>
 */
@Service
public class SoulmateChatService {

    private final ChatClient soulmateChatClient;

    public SoulmateChatService(ChatClient soulmateChatClient) {
        this.soulmateChatClient = soulmateChatClient;
    }

    public AiReply chat(String conversationId, String anonymizedUserName, String mood, String userMessage) {
        return soulmateChatClient.prompt()
                .system(system -> system
                        .text("""
                                너는 {userName} 님의 AI 친구야.
                                유저의 현재 기분은 '{mood}' 이야.
                                답변은 3문장 이내로, 반말로 친근하게 해.
                                유저가 이어서 보낼 만한 짧은 답장 후보(choices) 를 2~3개 함께 제안해.
                                이번 한 턴으로 너에 대한 호감도(affectionDelta) 가 -5~+5 사이에서 얼마나 변할지 정수로 추정해.
                                """)
                        .param("userName", anonymizedUserName)
                        .param("mood", mood))
                .user(userMessage)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(AiReply.class);
    }
}
