package kr.spartaclub.aifriends.chat.dto;

/**
 * Day 7 Step 9 — 셀카 요청 처리 결과.
 *
 * <p>{@code chat/service/SelcaService} 가 {@code AiChatService} 로 흘려보내는 응답.
 * 이미지 생성 성공/한도 초과/실패의 세 상태를 한 record 로 표현한다.</p>
 *
 * <p>필드 의미:
 * <ul>
 *   <li>{@code imageUrl} — 성공 시 정적 리소스 경로(예: {@code /uploads/portraits/selca-xxx.jpg}).
 *       한도 초과/실패면 {@code null}.</li>
 *   <li>{@code fallbackMessage} — 한도 초과/실패 시 LLM 응답을 덮어쓸 *캐릭터 인격 톤* 우회 메시지.
 *       성공이면 {@code null} (LLM 응답 그대로 흘려보냄).</li>
 *   <li>{@code quotaExceeded} — 한도 초과로 우회됐는지 여부. 응답 헤더 {@code X-Image-Quota-Exceeded}
 *       같은 *기술적 디테일* 을 흘려보낼 자리에서 사용한다.</li>
 * </ul>
 */
public record SelcaResult(
        String imageUrl,
        String fallbackMessage,
        boolean quotaExceeded
) {
    public static SelcaResult success(String imageUrl) {
        return new SelcaResult(imageUrl, null, false);
    }

    public static SelcaResult quotaExceeded(String fallbackMessage) {
        return new SelcaResult(null, fallbackMessage, true);
    }

    public static SelcaResult failed(String fallbackMessage) {
        return new SelcaResult(null, fallbackMessage, false);
    }
}
