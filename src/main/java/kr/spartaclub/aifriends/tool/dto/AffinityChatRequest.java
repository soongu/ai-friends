package kr.spartaclub.aifriends.tool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Day 11 Step 4 — {@code POST /api/tool/affinity/chat} 의 요청 바디.
 *
 * <p>학생은 캐릭터에게 "지금 우리 사이 어때?", "너 나 좋아해?" 같은 자연어 메시지를 던진다.
 * LLM 이 system 프롬프트와 메시지를 보고 등록된 {@code AffinityTool.getAffinity} 를 자율 호출,
 * 받은 score 와 level 을 캐릭터 톤에 맞게 자연스럽게 풀어 응답한다.</p>
 *
 * @param soulmateId 관계를 물어볼 캐릭터의 ID. null 금지.
 * @param message    유저 발화. 빈 문자열 금지.
 */
public record AffinityChatRequest(
        @NotNull(message = "soulmateId 는 필수입니다.")
        Long soulmateId,

        @NotBlank(message = "메시지를 입력해 주세요.")
        String message
) { }
