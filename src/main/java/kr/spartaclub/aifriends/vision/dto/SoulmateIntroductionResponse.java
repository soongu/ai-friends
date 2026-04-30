package kr.spartaclub.aifriends.vision.dto;

/**
 * Day 8 Step 6 — 캐릭터 자기소개 응답 DTO.
 *
 * <p>Day 7 에서 생성한 portrait URL 을 Day 8 의 Vision 입력으로 흘려보낸 결과 —
 * 캐릭터가 자기 자화상을 보고 한 자기소개 텍스트를 함께 돌려준다.</p>
 *
 * @param soulmateId   캐릭터 PK
 * @param name         캐릭터 표시 이름
 * @param portraitUrl  Vision 입력으로 사용된 portrait URL
 * @param introduction ChatModel 이 생성한 자기소개 텍스트 (한국어 2~3 문장)
 */
public record SoulmateIntroductionResponse(
        Long soulmateId,
        String name,
        String portraitUrl,
        String introduction
) {
}
