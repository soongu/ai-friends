package kr.spartaclub.aifriends.chat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Day 3 Step 2 — ChatClient 를 빈으로 등록하는 시작 지점.
 *
 * <p>Day 1~2 에서는 컨트롤러가 ChatClient.Builder 를 직접 받아 build() 했지만,
 * 오늘부터는 Config 에서 페르소나가 박힌 ChatClient 를 완제품으로 만들어두고
 * 각 서비스가 주입받아 쓰는 구조로 한 단계 올라간다.</p>
 *
 * <p>Day 5 Step 4 — ChatMemory + MessageChatMemoryAdvisor 를 등록해서
 * ChatClient 호출 직전에 이전 대화 이력이 자동으로 끼워들어가도록 만든다.
 * Service · Controller 코드는 그대로 두고, 빈 등록 한 군데에서 멀티턴이 살아난다.</p>
 */
@Configuration
public class ChatClientConfig {

    /**
     * Day 5 Step 4 — 슬라이딩 윈도우 기반 ChatMemory 빈.
     *
     * <p>{@link ChatMemoryRepository} 는 Day 5 Step 3 에서 등록한
     * {@code JdbcChatMemoryRepository} 가 자동 주입된다 (MySQL/H2 영속화).
     * maxMessages = 20 은 sliding window 의 상한 — 가장 최근 20 개 메시지만 LLM 컨텍스트로 흘러간다.
     * Step 6 에서 토큰 비용·기억 길이 트레이드오프를 따라 이 값을 조정한다.</p>
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
    }

    /**
     * 소꿉친구 페르소나용 ChatClient.
     *
     * <p>Day 5 Step 4 — defaultAdvisors 에 {@link MessageChatMemoryAdvisor} 를 등록해서
     * 모든 호출 직전에 ChatMemory 의 이력이 자동 주입되고, 응답 직후에 새 메시지가 자동 저장되도록 한다.
     * 호출부(Service)는 advisor 의 존재를 모른 채 한 줄로 호출한다.</p>
     *
     * <p>Day 5 Step 6 — defaultSystem 의 깡통 페르소나를 들어냈다.
     * 진짜 페르소나는 {@code prompts/soulmate/system-v1.st} 외부 파일에 있고,
     * {@link kr.spartaclub.aifriends.chat.service.SoulmateChatService#chat(Long, String)} 가
     * 호출 시점에 Soulmate 엔티티 컬럼을 슬롯에 박아 넣는다.
     * 페르소나가 늘어나면 @Bean 메서드 이름이 그대로 Qualifier 가 된다 (senpaiChatClient 등).</p>
     */
    @Bean
    public ChatClient soulmateChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
