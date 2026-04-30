package kr.spartaclub.aifriends.tool;

import kr.spartaclub.aifriends.domain.GameStateEntry;
import kr.spartaclub.aifriends.repository.GameStateEntryRepository;
import kr.spartaclub.aifriends.tool.dto.GameStateSnapshot;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Day 11 Step 3 — DB 영속화가 들어가는 두 번째 도구.
 *
 * <p>Step 2 의 {@code WeatherTool} 은 stub 상수만 돌려주는 "순수 함수" 였다.
 * {@code GameStateTool} 은 한 단계 더 들어간다 — 도구 함수 안에서 실제 JPA Repository 를
 * 호출해 DB 에 row 를 쌓고(saveGameState), 다시 꺼내(loadGameState) LLM 에게 돌려준다.</p>
 *
 * <p>핵심 가르침: <strong>{@code @Tool} 메서드 안에서 우리는 평소 Spring 컴포넌트처럼
 * Repository · Service · 외부 API 어떤 것이든 자유롭게 호출할 수 있다.</strong>
 * LLM 입장에서는 그저 "함수 시그니처 + JSON 응답" 일 뿐이고, 그 함수 안에서 무엇을 하든
 * LLM 은 알 필요가 없다 — 도구 호출이 단순한 stub 을 넘어 우리 앱의 진짜 기능과
 * 결합되는 순간이다.</p>
 *
 * <p>append-only 정책: 같은 playerId 라도 매번 새 row 를 쌓는다. update 가 아니다 —
 * 이전 저장본을 덮어쓰지 않고 시간선 그대로 보관해 학생이 "이 playerId 의 게임 진행 흐름을
 * 시간순으로 다시 살펴보고 싶다" 같은 요구가 생겼을 때 그대로 활용 가능하게 둔다.</p>
 */
@Component
public class GameStateTool {

    private final GameStateEntryRepository repository;

    public GameStateTool(GameStateEntryRepository repository) {
        this.repository = repository;
    }

    @Tool(description = "현재 게임 상태(마지막 유저 메시지, 마지막 캐릭터 응답, 진행한 턴 수) 를 저장한다. "
            + "유저가 '여기까지 저장해줘', '오늘 대화 기억해놔' 같은 요청을 할 때 호출하라.")
    public void saveGameState(
            @ToolParam(description = "플레이어 ID. 캐릭터의 호출자 식별자.")
            Long playerId,
            @ToolParam(description = "마지막으로 유저가 보낸 메시지 원문")
            String lastUserMessage,
            @ToolParam(description = "마지막으로 캐릭터가 답한 메시지 원문")
            String lastAiMessage,
            @ToolParam(description = "지금까지 진행된 대화의 턴 수 (저장 시점 기준)")
            int turnCount
    ) {
        GameStateEntry entry = new GameStateEntry(
                null, playerId, lastUserMessage, lastAiMessage, turnCount, null);
        repository.save(entry);
    }

    @Tool(description = "해당 playerId 의 가장 최근 게임 상태를 불러온다. "
            + "유저가 '저번에 어디까지 했지?', '우리 무슨 얘기했더라?' 같이 물으면 호출하라. "
            + "기록이 없으면 found=false 인 빈 Snapshot 이 돌아오니, 그때는 캐릭터가 '기억 안 나' 라고 자연스럽게 답해라.")
    public GameStateSnapshot loadGameState(
            @ToolParam(description = "조회할 플레이어 ID")
            Long playerId
    ) {
        return repository.findFirstByPlayerIdOrderByCreatedAtDesc(playerId)
                .map(entry -> new GameStateSnapshot(
                        true,
                        entry.getLastUserMessage(),
                        entry.getLastAiMessage(),
                        entry.getTurnCount()))
                .orElseGet(GameStateSnapshot::empty);
    }
}
