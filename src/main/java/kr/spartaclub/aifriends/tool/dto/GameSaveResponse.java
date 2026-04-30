package kr.spartaclub.aifriends.tool.dto;

/**
 * Day 11 Step 3 — {@code POST /api/tool/game/save} 의 응답 바디.
 *
 * <p>저장이 끝났음을 알리는 단순 ack — 그대로 LLM 호출이 아니므로 자연어 응답은 없다.
 * 학생이 응답 본문에서 "잘 저장됐구나" 를 시각적으로 확인할 수 있도록 playerId 와
 * turnCount 를 한 번 더 그대로 돌려준다.</p>
 */
public record GameSaveResponse(
        Long playerId,
        int turnCount
) { }
