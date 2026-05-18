package kr.spartaclub.aifriends.tool.demo;

/**
 * Day 12 Step 3 — 라이브 시연 부스 응답 바디.
 *
 * <p>{@code aiMessage} 는 LLM 이 자율 루프를 한 차례 마치고 (또는 강사가 강제 종료한 시점에)
 * 돌려준 한 턴 대사다. <strong>시연의 본질은 응답 바디가 아니라 콘솔 로그</strong>이다 —
 * 도구 호출 횟수와 누적 토큰은 도구 메서드 내부 {@code log.info} 와 Spring AI 의
 * 컨텍스트 카운터로 강사 화면에서 직접 관찰한다.</p>
 */
public record RunawayDemoResponse(
        String aiMessage
) { }
