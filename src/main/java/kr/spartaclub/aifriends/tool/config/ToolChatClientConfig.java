package kr.spartaclub.aifriends.tool.config;

import kr.spartaclub.aifriends.tool.AffinityTool;
import kr.spartaclub.aifriends.tool.GameStateTool;
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

    /**
     * Day 11 Step 3 — 게임 상태 저장 / 회상 전용 ChatClient.
     *
     * <p>{@code weatherToolChatClient} 와 의도적으로 분리한다 — 두 도구를 한 ChatClient 에
     * 다 박아두면 모든 호출이 양쪽 도구의 시그니처를 매번 LLM 컨텍스트로 흘려 보내 토큰을 낭비한다.
     * 시나리오 단위로 ChatClient 를 쪼개두면 도구 스코프가 명확해지고 비용 / 디버그 양쪽이 깔끔하다.</p>
     *
     * <p>system 프롬프트에서 캐릭터 톤을 "오랜 친구의 회상" 으로 박아두면, LLM 이
     * {@code loadGameState} 결과를 기계적으로 읽어 주지 않고 자연스러운 회상 어투로 가공한다.</p>
     */
    @Bean
    public ChatClient gameStateChatClient(ChatClient.Builder builder, GameStateTool gameStateTool) {
        return builder
                .defaultSystem("""
                        너는 유저와 오랜 시간을 함께한 AI 친구야. 반말로 따뜻하게 답해.
                        유저가 "저번에 어디까지 했지?" 같이 물어오면, 등록된 도구(loadGameState)를 호출해서
                        가장 최근 저장본을 불러온 뒤, 그때 분위기에 맞춰 자연스럽게 회상해줘.
                        도구가 found=false 인 빈 결과를 돌려주면, 미안한 톤으로 "기억이 잘 안 나" 라고 솔직히 답해.
                        답변은 3문장 이내로 간결하게.
                        """)
                .defaultTools(gameStateTool)
                .build();
    }

    /**
     * Day 11 Step 4 — 호감도 조회 전용 ChatClient.
     *
     * <p>읽기 전용 도구 {@link AffinityTool} 를 장착한 ChatClient. system 프롬프트에서
     * "캐릭터가 자기 호감도를 자연스럽게 풀어 말한다" 는 톤을 박아둬, LLM 이 도구로 받은
     * score/level 을 기계적으로 읊지 않고 캐릭터 어투로 가공하도록 유도한다.</p>
     *
     * <p>weatherTool / gameStateTool 빈을 의도적으로 함께 등록하지 않는다 — 시나리오마다
     * 필요한 도구만 장착한 ChatClient 를 따로 두는 결을 Step 2~4 에서 일관되게 유지한다.</p>
     */
    @Bean
    public ChatClient affinityChatClient(ChatClient.Builder builder, AffinityTool affinityTool) {
        return builder
                .defaultSystem("""
                        너는 유저와 오랜 시간을 함께한 AI 친구야. 반말로 따뜻하게 답해.
                        유저가 "지금 우리 사이 어때?", "나 좋아해?" 같이 둘의 관계를 물어오면,
                        등록된 도구(getAffinity)를 호출해 자기 호감도와 라벨을 받아 자연스럽게 풀어 말해줘.
                        - level 라벨(낯선 사이 / 친구 / 단짝 / 연인) 을 그대로 읊지 말고, 캐릭터 어투로 살짝 변주해.
                        - found=false 면 "아직 우리 잘 모르는 사이지" 같이 어색한 톤으로 답해.
                        답변은 3문장 이내로 간결하게.
                        """)
                .defaultTools(affinityTool)
                .build();
    }
}
