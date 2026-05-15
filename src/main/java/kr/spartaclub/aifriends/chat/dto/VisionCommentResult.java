package kr.spartaclub.aifriends.chat.dto;

/**
 * Day 8 후속 retrofit — 사용자 업로드 이미지에 대한 캐릭터 코멘트 처리 결과.
 *
 * <p>Day 7 의 {@link SelcaResult} 와 *대칭* 으로 박힌 record. 셀카는 *LLM 텍스트 + AI 가 만든 이미지* 의
 * 두 출력을 합치고, 비전은 *LLM 의 vision 모드 응답 한 줄* 로 채팅 본문을 *대체* 한다 — 입력 모달리티가
 * 바뀌면 출력 합성 결도 달라지는 자리.</p>
 *
 * <p>필드 의미:
 * <ul>
 *   <li>{@code aiComment} — 성공 시 캐릭터가 사용자 이미지를 보고 한 코멘트 (한국어 한~두 문장).
 *       한도 초과/실패면 {@code null}.</li>
 *   <li>{@code fallbackMessage} — 한도 초과/실패 시 LLM 응답을 덮어쓸 *캐릭터 인격 톤* 우회 메시지.
 *       성공이면 {@code null}.</li>
 *   <li>{@code quotaExceeded} — Vision 일일 호출 한도 초과 여부. 운영 시 응답 헤더
 *       {@code X-Vision-Quota-Exceeded} 같은 *기술적 디테일* 을 흘려보낼 자리에서 사용한다.</li>
 * </ul>
 */
public record VisionCommentResult(
        String aiComment,
        String fallbackMessage,
        boolean quotaExceeded
) {
    public static VisionCommentResult success(String aiComment) {
        return new VisionCommentResult(aiComment, null, false);
    }

    public static VisionCommentResult quotaExceeded(String fallbackMessage) {
        return new VisionCommentResult(null, fallbackMessage, true);
    }

    public static VisionCommentResult failed(String fallbackMessage) {
        return new VisionCommentResult(null, fallbackMessage, false);
    }
}
