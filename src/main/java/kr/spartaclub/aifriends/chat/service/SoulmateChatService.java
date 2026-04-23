package kr.spartaclub.aifriends.chat.service;

import org.springframework.ai.chat.client.ChatClient;
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
 */
@Service
public class SoulmateChatService {

    private final ChatClient soulmateChatClient;

    public SoulmateChatService(ChatClient soulmateChatClient) {
        this.soulmateChatClient = soulmateChatClient;
    }

    public String chat(String anonymizedUserName, String mood, String userMessage) {
        return soulmateChatClient.prompt()
                .system(system -> system
                        .text("""
                                너는 {userName} 님의 AI 친구야.
                                유저의 현재 기분은 '{mood}' 이야.
                                답변은 3문장 이내로, 반말로 친근하게 해.
                                """)
                        .param("userName", anonymizedUserName)
                        .param("mood", mood))
                .user(userMessage)
                .call()
                .content();
    }
}
