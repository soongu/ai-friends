package kr.spartaclub.aifriends.tool.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Day 11 Step 3 — {@code POST /api/tool/game/recall} 의 요청 바디.
 *
 * <p>유저가 캐릭터에게 "저번에 우리 어디까지 했지?" 를 묻는 시점 — 이 한 번만으로
 * LLM 이 등록된 {@code GameStateTool.loadGameState} 를 자율적으로 호출하고,
 * 돌려받은 {@link GameStateSnapshot} 을 기반으로 캐릭터 톤의 회상 응답을 만들어낸다.</p>
 */
public record GameRecallRequest(
        @NotNull(message = "playerId 는 필수입니다.")
        Long playerId
) { }
