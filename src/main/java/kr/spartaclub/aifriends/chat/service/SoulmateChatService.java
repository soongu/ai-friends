package kr.spartaclub.aifriends.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Day 3 Step 2 — 소꿉친구 페르소나 ChatClient 를 사용하는 서비스.
 *
 * <p>이 서비스의 코드에는 "소꿉친구" 라는 단어가 단 한 번도 등장하지 않는다.
 * 페르소나는 ChatClientConfig 의 defaultSystem 에 박혀 있고,
 * 이 서비스는 그냥 "주입받은 ChatClient 로 유저 메시지를 호출" 할 뿐이다.
 * 프롬프트와 비즈니스 로직의 관심사 분리가 여기서 시작된다.</p>
 */
@Service
public class SoulmateChatService {

    private final ChatClient soulmateChatClient;

    public SoulmateChatService(ChatClient soulmateChatClient) {
        this.soulmateChatClient = soulmateChatClient;
    }

    public String chat(String userMessage) {
        return soulmateChatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }
}
