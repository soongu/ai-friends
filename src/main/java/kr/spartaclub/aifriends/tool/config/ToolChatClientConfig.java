package kr.spartaclub.aifriends.tool.config;

import kr.spartaclub.aifriends.tool.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Day 11 Step 2 — Tool Calling 전용 ChatClient 빈.
 *
 * <p>Day 3 에서 만든 {@code soulmateChatClient} 와는 분리한다 —
 * 페르소나 ChatClient 에 모든 도구를 다 박아두면 다른 데모(Day 5 ChatMemory, Day 6 Streaming)
 * 가 의도치 않게 도구 호출 비용을 떠안게 된다. "도구가 필요한 시나리오 전용 ChatClient"
 * 라는 결을 잡아두면 학생이 도구 등록 범위를 직관적으로 이해할 수 있다.</p>
 *
 * <p>{@code defaultTools(weatherTool)} 한 줄로 LLM 은 {@code WeatherTool} 의
 * {@code @Tool} 메서드 시그니처를 알게 되고, 사용자 프롬프트에 맞춰 자율적으로 호출한다.
 * 우리는 그 결과를 받아 자연스러운 캐릭터 응답을 돌려주기만 하면 된다.</p>
 */
@Configuration
public class ToolChatClientConfig {

    @Bean
    public ChatClient weatherToolChatClient(ChatClient.Builder builder, WeatherTool weatherTool) {
        return builder
                .defaultSystem("""
                        너는 유저에게 오늘의 날씨와 옷차림을 추천해주는 친근한 AI 친구야.
                        반말로 따뜻하게 답하고, 답변은 3문장 이내로 간결하게 해.
                        도시별 실제 날씨가 필요하면 등록된 도구(getCurrentWeather)를 자유롭게 호출해.
                        """)
                .defaultTools(weatherTool)
                .build();
    }
}
