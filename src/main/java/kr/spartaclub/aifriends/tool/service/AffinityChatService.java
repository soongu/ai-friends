package kr.spartaclub.aifriends.tool.service;

import kr.spartaclub.aifriends.tool.AffinityTool;
import kr.spartaclub.aifriends.tool.dto.AffinityChatResponse;
import kr.spartaclub.aifriends.tool.dto.AffinityInfo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Day 11 Step 4 — 호감도 대화 서비스.
 *
 * <p>Step 3 의 {@code GameStateRecallService.recall} 과 같은 결 — LLM 이 자율적으로
 * 등록된 도구를 호출하도록 사용자 메시지만 흘려보내고, 우리는 도구 호출 여부를 알 필요가 없다.
 * 응답에는 LLM 의 자연어와, 같은 도구를 한 번 더 직접 호출해 받은 score/level 을 디버그
 * 신호로 함께 끼워준다 — Step 2 {@code WeatherToolChatService} 와 동일한 학습 결.</p>
 *
 * <p>{@code @Qualifier("affinityChatClient")} 로 명시 주입하는 이유는 도구 스코프 격리 —
 * weatherToolChatClient / gameStateChatClient 와 도구가 섞이지 않게 한다.</p>
 */
@Service
public class AffinityChatService {

    private final ChatClient affinityChatClient;
    private final AffinityTool affinityTool;

    public AffinityChatService(
            @Qualifier("affinityChatClient") ChatClient affinityChatClient,
            AffinityTool affinityTool
    ) {
        this.affinityChatClient = affinityChatClient;
        this.affinityTool = affinityTool;
    }

    public AffinityChatResponse chat(Long soulmateId, String message) {
        String userPrompt = "soulmateId=%d 인 유저야. %s".formatted(soulmateId, message);

        String aiMessage = affinityChatClient.prompt()
                .user(userPrompt)
                .call()
                .content();

        // 학습용 디버그 신호 — LLM 이 실제로 호출했을 도구를 한 번 더 직접 호출해 score/level 을 노출.
        AffinityInfo info = affinityTool.getAffinity(soulmateId);

        return new AffinityChatResponse(
                soulmateId,
                aiMessage,
                info.score(),
                info.level());
    }
}
