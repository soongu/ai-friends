package kr.spartaclub.aifriends.hello;

/**
 * Day 3 과제 2 — A/B 분기 응답 DTO.
 *
 * <p>promptVersion 을 응답 본문에 명시함으로써 클라이언트·QA·로그 수집기 어디서든
 * "어떤 프롬프트가 응답한 결과인가" 를 한눈에 식별할 수 있게 한다.
 * A/B 실험에서 가장 먼저 갖춰야 할 최소 관측 지표.</p>
 */
public record HelloAbResponse(
        String promptVersion,  // "v1" 또는 "v2"
        String userName,       // 익명 ID (tutor-student-1)
        String topicTag,       // 오늘의 주제 태그
        String reply           // 모델 응답 본문
) {
}
