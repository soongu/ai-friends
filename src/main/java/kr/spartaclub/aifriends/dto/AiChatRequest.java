package kr.spartaclub.aifriends.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 프론트엔드에서 사용자가 이성친구에게 보낼 메시지를 담아 서버로 요청할 때 사용하는 DTO입니다.
 *
 * <p>Day 8 후속 retrofit — 사용자가 이미지를 첨부한 경우 {@code imageUrl} 에 정적 리소스 경로
 * (예: {@code /uploads/portraits/upload-xxx.png}) 가 담긴다. {@code null} 이면 일반 텍스트 채팅.
 * 옵션 필드라 *기존 컨트랙트 (2-arg)* 는 그대로 살아 있어 학생 데모 무파괴.</p>
 */
public record AiChatRequest(
        /** 대화할 이성친구의 고유 ID. 반드시 존재해야 합니다. */
        @NotNull(message = "이성친구 ID는 필수입니다.")
        Long soulmateId,

        /** 사용자가 입력한 메시지 내용. 공백이나 빈 문자열은 허용하지 않습니다. */
        @NotBlank(message = "메시지를 입력해 주세요.")
        String userMessage,

        /**
         * (Day 8) 사용자가 첨부한 이미지의 정적 리소스 경로. {@code null}/blank 면 텍스트 채팅.
         * 일반적으로 {@code POST /api/vision/uploads} 응답의 {@code publicPath} 가 그대로 들어온다.
         */
        String imageUrl
) {
    /**
     * 호환 생성자 — Day 7 시점까지의 호출처를 그대로 살린다 (학생 데모 무파괴).
     * 컨트랙트가 깨지지 않게 *옵션 필드를 null 로 채우는* 결을 명시적으로 보여주는 자리.
     */
    public AiChatRequest(Long soulmateId, String userMessage) {
        this(soulmateId, userMessage, null);
    }
}
