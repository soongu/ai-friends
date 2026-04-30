package kr.spartaclub.aifriends.tool.dto;

/**
 * Day 11 Step 3 — {@code POST /api/tool/game/recall} 의 응답 바디.
 *
 * <p>LLM 이 {@code GameStateTool.loadGameState} 를 자율 호출해 받아낸 Snapshot 을 기반으로
 * 캐릭터 톤으로 회상한 한 턴 대사를 그대로 돌려준다. 학생이 이 응답만 보고
 * "도구 호출 → DB 조회 → LLM 자연어 가공" 의 흐름이 끝까지 동작했는지 확인할 수 있다.</p>
 */
public record GameRecallResponse(
        Long playerId,
        String aiMessage
) { }
