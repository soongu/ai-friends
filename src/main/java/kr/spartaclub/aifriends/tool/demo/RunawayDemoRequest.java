package kr.spartaclub.aifriends.tool.demo;

import jakarta.validation.constraints.NotBlank;

/**
 * Day 12 Step 3 — 라이브 시연 부스 요청 바디.
 *
 * <p>자연어 한 줄만 받는다. Day 11 의 시나리오별 컨트롤러 (city / soulmateId+message / playerId)
 * 와 달리 본 시연은 "사용자가 어떤 한 줄을 던졌을 때 LLM 이 자율적으로 어느 도구를 어떤 횟수로
 * 호출하는지" 가 관전 포인트라, 구조화된 필드 없이 자유 메시지만 받는다.</p>
 *
 * <p>시연 1 (무한 루프 + 토큰 폭발) · 시연 2 (도구 남용) · 시연 3 (권한 누수) 의 세 프롬프트
 * 모두 본 엔드포인트로 던진다 — 변하는 건 message 한 줄이다.</p>
 */
public record RunawayDemoRequest(
        @NotBlank(message = "메시지를 입력해 주세요.")
        String message
) { }
