package kr.spartaclub.aifriends.image.dto;

/**
 * Day 7 Step 5 — 이미지 생성 결과 DTO.
 *
 * @param localPath          우리 서버의 정적 리소스 경로 (예: {@code /uploads/portraits/abc.jpg}).
 *                           프론트가 그대로 {@code <img src="...">} 에 박아 쓰면 된다.
 * @param externalUrl        프로바이더가 발급한 원본 URL (참고/디버깅 용도).
 * @param prompt             실제 LLM 에 흘려보낸 프롬프트 본문 (감사 로그 · 학생 디버깅용).
 * @param modelName          사용된 모델 식별자 (예: {@code pollinations-flux}, {@code openai-dall-e-3-standard}).
 * @param estimatedCostUsd   1회 호출 추정 비용. 학습용 하드코딩 매핑이며, 실제 청구는 프로바이더 대시보드 기준.
 */
public record ImageGenerationResult(
        String localPath,
        String externalUrl,
        String prompt,
        String modelName,
        double estimatedCostUsd
) {
}
