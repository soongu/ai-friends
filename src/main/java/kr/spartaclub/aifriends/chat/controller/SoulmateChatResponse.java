package kr.spartaclub.aifriends.chat.controller;

/**
 * Day 3 Step 3 — 소꿉친구 페르소나 응답 DTO.
 *
 * <p>{@code aiMessage} 한 필드만 담아 {@code ApiResponse&lt;T&gt;} 게이트(CLAUDE.md 4-1)와 정합한다.
 * Day 4 의 캐릭터 응답 record(예: {@code AiReply(aiMessage, choices, affectionDelta)}) 와
 * 필드 이름(aiMessage) 을 일치시켜, 향후 구조화 출력으로 자연스럽게 확장할 수 있도록 했다.</p>
 */
public record SoulmateChatResponse(String aiMessage) {
}
