package kr.spartaclub.aifriends.tool.dto;

/**
 * Day 11 Step 3 — {@link kr.spartaclub.aifriends.tool.GameStateTool#loadGameState} 의 반환 DTO.
 *
 * <p>도구 함수가 반환하는 구조체는 LLM 에게 그대로 JSON 으로 전달된다.
 * 따라서 필드명을 LLM 이 읽기 쉬운 의미 단위 (lastUserMessage, lastAiMessage, turnCount, found)
 * 로 정리해두면 LLM 이 자연어로 회상해줄 때 의미를 잘 살린다.</p>
 *
 * <p>{@code found = false} 인 경우는 "저장된 기록이 없다" 는 신호다 —
 * null 을 반환하지 않고 명시적 boolean 으로 알리는 게 LLM 입장에선 훨씬 다루기 쉽다.
 * 빈 결과를 LLM 이 "기억 안 나" 라고 자연스럽게 가공해줄 단서로 쓴다.</p>
 *
 * @param found            저장된 기록이 있으면 true, 없으면 false
 * @param lastUserMessage  마지막으로 유저가 보낸 메시지 (없으면 빈 문자열)
 * @param lastAiMessage    마지막으로 캐릭터가 보낸 메시지 (없으면 빈 문자열)
 * @param turnCount        대화가 몇 턴까지 진행됐는지 (없으면 0)
 */
public record GameStateSnapshot(
        boolean found,
        String lastUserMessage,
        String lastAiMessage,
        int turnCount
) {
    /**
     * 저장된 기록이 없는 playerId 에 대해 일관된 빈 신호를 만들어준다.
     * "기록 없음" 도 LLM 입장에선 정상 응답이라는 결을 강조한다.
     */
    public static GameStateSnapshot empty() {
        return new GameStateSnapshot(false, "", "", 0);
    }
}
