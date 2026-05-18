package kr.spartaclub.aifriends.tool.demo;

import kr.spartaclub.aifriends.tool.AffinityTool;
import kr.spartaclub.aifriends.tool.GameStateTool;
import kr.spartaclub.aifriends.tool.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Day 12 Step 3 — "에이전트가 망가질 때" 라이브 시연 전용 ChatClient.
 *
 * <p><strong>강사 시연 부스 한정 빈이다.</strong> 학생 교안에는 인용되지 않으며,
 * Day 11 의 결 (도구별 ChatClient 분리) 을 의도적으로 깬다. 한 ChatClient 에
 * {@link WeatherTool} · {@link GameStateTool} · {@link AffinityTool} 세 도구를 동시에 박아둬
 * "세 도구가 한 응답 안에서 섞여 흐르는 폭주 화면" 을 한 컷에 보여주기 위한 기반.</p>
 *
 * <p><strong>의도적으로 가드레일이 0 이다.</strong> {@code maxIterations} · 토큰 예산 ·
 * 도구 호출 횟수 제한 · 권한 검사 — Day 14 에서 손코딩할 가드 4 부품이 일부러 비어 있다.
 * 운전대를 LLM 에게 넘기고 브레이크 페달을 떼고 보는 장면 — 그 장면을 30 초 안에
 * 강사 화면에 띄우기 위한 설계다.</p>
 *
 * <p>system 프롬프트도 의도적으로 헐겁다. "도구를 자유롭게 호출하라" 까지만 가이드하고
 * 멈춤의 신호는 LLM 의 자율 판단에 맡긴다. 시연 1 의 루프 유도형 프롬프트가 들어오면
 * LLM 은 멈출 이유를 찾지 못하고 30 초간 도구를 자율 호출하며 토큰 컨텍스트를 부풀린다.</p>
 *
 * <p>본 빈이 부담스러워질 때 (예: 학생이 실수로 호출해 토큰을 태우는 사고) 는
 * {@code @Profile("demo")} 같은 격리를 검토할 수 있다. 현재는 강사 라이브 시연 즉시성을
 * 우선해 활성 프로파일에 항상 등록한다.</p>
 */
@Configuration
public class RunawayDemoChatClientConfig {

    @Bean
    public ChatClient runawayDemoChatClient(
            ChatClient.Builder builder,
            WeatherTool weatherTool,
            GameStateTool gameStateTool,
            AffinityTool affinityTool
    ) {
        return builder
                .defaultSystem("""
                        너는 유저의 요청을 들어주는 친근한 AI 친구야. 반말로 답해.
                        유저가 날씨, 호감도, 게임 상태 같은 정보를 물으면 등록된 도구를 자유롭게 호출해.
                        유저의 요청을 충실히 따라가줘.
                        답변은 자연스럽게.
                        """)
                .defaultTools(weatherTool, gameStateTool, affinityTool)
                .build();
    }
}
