package kr.spartaclub.aifriends.tool.service;

import kr.spartaclub.aifriends.tool.GameStateTool;
import kr.spartaclub.aifriends.tool.dto.GameRecallResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Day 11 Step 3 — 게임 상태 저장 / 회상 서비스.
 *
 * <p>두 메서드의 결이 의도적으로 다르다.</p>
 *
 * <ul>
 *   <li>{@code save} 는 LLM 을 거치지 않고 {@link GameStateTool#saveGameState} 를 직접 호출한다.
 *       "저장" 은 명시적 액션이라 LLM 의 자율 판단이 끼어들 필요가 없고, 도구 본체가 그저
 *       "Repository 한 번 호출하는 평범한 메서드" 라는 점을 학생이 직접 느끼게 한다.</li>
 *   <li>{@code recall} 은 {@code gameStateChatClient} 에 프롬프트만 던진다 — LLM 이 등록된
 *       {@code loadGameState} 도구를 자율적으로 호출해, 돌려받은 Snapshot 을 기반으로 캐릭터 톤의
 *       회상 응답을 만들어낸다. 호출부에서는 도구 호출 여부조차 모른다.</li>
 * </ul>
 *
 * <p>{@code @Qualifier("gameStateChatClient")} 로 명시 주입하는 이유는 도구 스코프 격리 —
 * Step 2 의 {@code weatherToolChatClient} 와 같은 컨텍스트에 두 도구를 모두 등록하면
 * 모든 호출에 양쪽 도구의 시그니처가 함께 흘러가 토큰을 낭비한다. 시나리오마다 필요한 도구만
 * 장착한 ChatClient 를 따로 두는 결을 박아둔다.</p>
 */
@Service
public class GameStateRecallService {

    private final GameStateTool gameStateTool;
    private final ChatClient gameStateChatClient;

    public GameStateRecallService(
            GameStateTool gameStateTool,
            @Qualifier("gameStateChatClient") ChatClient gameStateChatClient
    ) {
        this.gameStateTool = gameStateTool;
        this.gameStateChatClient = gameStateChatClient;
    }

    /**
     * 게임 상태를 저장한다 — LLM 우회, Tool 직접 호출.
     */
    public void save(Long playerId, String lastUserMessage, String lastAiMessage, int turnCount) {
        gameStateTool.saveGameState(playerId, lastUserMessage, lastAiMessage, turnCount);
    }

    /**
     * 캐릭터에게 "저번에 어디까지 했지?" 를 묻는다 — LLM 이 도구 자율 호출.
     */
    public GameRecallResponse recall(Long playerId) {
        String userPrompt = """
                나 playerId=%d 인데, 저번에 우리 어디까지 얘기했었지?
                필요하면 등록된 도구로 마지막 게임 상태를 불러와서 그때 분위기에 맞춰 자연스럽게 회상해줘.
                """.formatted(playerId);

        String aiMessage = gameStateChatClient.prompt()
                .user(userPrompt)
                .call()
                .content();

        return new GameRecallResponse(playerId, aiMessage);
    }
}
