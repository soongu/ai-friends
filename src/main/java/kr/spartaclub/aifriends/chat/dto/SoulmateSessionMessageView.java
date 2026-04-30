package kr.spartaclub.aifriends.chat.dto;

/**
 * Day 5 Step 5 — 한 conversationId 의 메시지 한 개를 외부로 노출할 때 쓰는 뷰.
 *
 * <p>Spring AI 의 {@code Message} 객체를 직접 응답으로 흘리면 내부 타입(AssistantMessage 등) 이
 * 컨트롤러 응답 스키마로 새어 나가므로, 학생들에게도 익숙한 role/content 두 필드만 노출한다.
 * role 은 {@code USER}, {@code ASSISTANT}, {@code SYSTEM}, {@code TOOL} 중 하나의 소문자.</p>
 */
public record SoulmateSessionMessageView(String role, String content) {
}
