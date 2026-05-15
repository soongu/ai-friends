package kr.spartaclub.aifriends.chat.dto;

/**
 * Day 5 Step 5 — 소꿉친구 채팅 응답.
 *
 * <p>Day 4 Step 5 까지의 응답은 {@link AiReply} 만 내려갔지만,
 * Day 5 부터는 ChatMemory 가 어느 세션(conversationId) 위에 메시지를 누적했는지를 함께 내려줘야
 * 클라이언트가 다음 호출에 같은 conversationId 를 들고 와 멀티턴을 이어갈 수 있다.</p>
 *
 * <p>conversationId 는 첫 호출 시 서버가 UUID 로 발급하고, 이후엔 클라이언트가 그대로 들고 다닌다.</p>
 */
public record SoulmateChatResponse(String conversationId, AiReply reply) {
}
