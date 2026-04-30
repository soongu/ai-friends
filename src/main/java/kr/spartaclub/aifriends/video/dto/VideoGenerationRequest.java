package kr.spartaclub.aifriends.video.dto;

/**
 * Day 10 — 비디오 생성 요청 DTO (선택 실습).
 *
 * <p>{@code POST /api/video/generate-async} 의 JSON 본문을 매핑한다.
 * 비디오 생성은 비용이 텍스트 호출의 *수천 배* 이므로 학생 실습용 stub 흐름에서도
 * "5초·720p" 와 같이 *명시적인 길이/해상도* 를 강제한다 — 사용자 카드를 보호하는 결.</p>
 *
 * <p>각 필드의 검증은 컨트롤러에서
 * {@link kr.spartaclub.aifriends.video.exception.VideoException} 으로 던져
 * 표준 에러 응답으로 변환된다.</p>
 *
 * @param prompt          생성할 비디오의 프롬프트 (필수)
 * @param durationSeconds 생성할 비디오의 길이 (1~10 초)
 * @param resolution      해상도 — "480p" / "720p" / "1080p" 중 하나
 */
public record VideoGenerationRequest(
        String prompt,
        int durationSeconds,
        String resolution) {
}
