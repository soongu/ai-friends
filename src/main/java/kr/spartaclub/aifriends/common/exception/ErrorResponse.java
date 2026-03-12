package kr.spartaclub.aifriends.common.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {
    private final LocalDateTime timestamp; // 에러가 발생한 시간 (예: 2024-03-01T12:00:00)
    private final int status;              // HTTP 상태 코드 (예: 400, 404)
    private final String error;            // HTTP 상태 코드 이름 (예: BAD_REQUEST)
    private final String code;             // 기획 요구사항 및 클라이언트 식별을 위해 정의한 커스텀 에러 코드 (예: M001)
    private final String message;          // 에러 원인에 대한 상세 메시지 (예: 이미 존재하는 이메일입니다.)
    private final String path;             // 에러가 발생한 API 요청 경로 (예: /api/auth/signup)
}