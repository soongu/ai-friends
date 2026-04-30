package kr.spartaclub.aifriends.tool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Day 11 Step 3 — {@code POST /api/tool/game/save} 의 요청 바디.
 *
 * <p>이 엔드포인트는 LLM 을 거치지 않고 컨트롤러 → 서비스 → {@code GameStateTool.saveGameState} 를
 * 직접 호출한다. "저장" 은 명시적 액션이라 LLM 의 자율 판단이 끼어들 필요가 없다 —
 * 회상(recall) 쪽에서만 LLM 이 도구 호출 여부를 스스로 결정하게 둔다.</p>
 */
public record GameSaveRequest(
        @NotNull(message = "playerId 는 필수입니다.")
        Long playerId,

        @NotBlank(message = "마지막 유저 메시지를 입력해 주세요.")
        String lastUserMessage,

        @NotBlank(message = "마지막 캐릭터 메시지를 입력해 주세요.")
        String lastAiMessage,

        @PositiveOrZero(message = "턴 수는 0 이상이어야 합니다.")
        int turnCount
) { }
