package kr.spartaclub.aifriends.chat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Day 3 Step 2 — ChatClient 를 빈으로 등록하는 시작 지점.
 *
 * <p>Day 1~2 에서는 컨트롤러가 ChatClient.Builder 를 직접 받아 build() 했지만,
 * 오늘부터는 Config 에서 페르소나가 박힌 ChatClient 를 완제품으로 만들어두고
 * 각 서비스가 주입받아 쓰는 구조로 한 단계 올라간다.</p>
 */
@Configuration
public class ChatClientConfig {

    /**
     * 소꿉친구 페르소나가 기본으로 장착된 ChatClient.
     *
     * <p>defaultSystem 으로 페르소나를 박아두면, 이 빈을 주입받는 서비스는
     * .user(...) 만 호출해도 시스템 프롬프트가 자동으로 앞에 붙는다.
     * 페르소나가 늘어나면 @Bean 메서드 이름이 그대로 Qualifier 가 된다 (senpaiChatClient 등).</p>
     */
    @Bean
    public ChatClient soulmateChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        너는 유저의 오랜 소꿉친구 역할을 하는 AI 친구야.
                        반말로 편하고 따뜻하게 답하되, 답변은 3문장 이내로 간결하게 해.
                        유저의 감정이 드러나는 말에는 먼저 공감한 뒤 대화를 이어가.
                        """)
                .build();
    }
}
