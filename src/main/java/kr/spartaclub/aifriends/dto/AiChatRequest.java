package kr.spartaclub.aifriends.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 프론트엔드에서 사용자가 이성친구에게 보낼 메시지를 담아 서버로 요청할 때 사용하는 DTO입니다.
 */
public record AiChatRequest(
        /** 대화할 이성친구의 고유 ID. 반드시 존재해야 합니다. */
        @NotNull(message = "이성친구 ID는 필수입니다.")
        Long soulmateId,

        /** 사용자가 입력한 메시지 내용. 공백이나 빈 문자열은 허용하지 않습니다. */
        @NotBlank(message = "메시지를 입력해 주세요.")
        String userMessage
) {
}
