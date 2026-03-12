package kr.spartaclub.aifriends.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "E001", "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "E002", "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E003", "서버 내부 오류가 발생했습니다."),

    // Soulmate
    SOULMATE_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "이성친구를 찾을 수 없습니다."),

    // Chat / Gemini
    MESSAGE_REQUIRED(HttpStatus.BAD_REQUEST, "G001", "메시지를 입력해 주세요."),
    AI_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "G002", "AI가 일시적으로 응답하지 않습니다."),
    RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "G003", "잠시 후 다시 시도해 주세요."),
    AI_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "G004", "AI 서비스 설정 오류가 발생했습니다."),
    REQUEST_TIMEOUT(HttpStatus.BAD_GATEWAY, "G005", "요청 시간이 초과되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
