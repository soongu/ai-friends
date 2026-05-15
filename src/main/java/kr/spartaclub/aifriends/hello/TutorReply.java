package kr.spartaclub.aifriends.hello;

/**
 * Day 3 과제 1 — {@code /api/hello-ai/v3} 응답 DTO.
 *
 * <p>{@code topicTag} (요청한 주제 라벨) 와 {@code reply} (LLM 의 답변 본문) 두 필드만 담는 최소 record.
 * promptVersion / latencyMs 같은 운영 라벨은 과제 2 의 {@link HelloAbResponse} 와 Day 1 의 {@link HelloResponse} 가
 * 각자 담당하므로 v3 의 책임 범위에서는 의도적으로 제외했다.</p>
 */
public record TutorReply(String topicTag, String reply) {
}
