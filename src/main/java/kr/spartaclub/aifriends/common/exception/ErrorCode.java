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
    SOULMATE_INVALID_PRESET(HttpStatus.BAD_REQUEST, "S002", "선택할 수 있는 캐릭터 프리셋이 아닙니다."),
    SOULMATE_CUSTOM_PROMPT_REQUIRED(HttpStatus.BAD_REQUEST, "S003", "커스텀 외모를 선택했다면 외모 prompt 를 입력해야 합니다."),

    // Chat / Gemini
    MESSAGE_REQUIRED(HttpStatus.BAD_REQUEST, "G001", "메시지를 입력해 주세요."),
    AI_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "G002", "AI가 일시적으로 응답하지 않습니다."),
    RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "G003", "잠시 후 다시 시도해 주세요."),
    AI_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "G004", "AI 서비스 설정 오류가 발생했습니다."),
    REQUEST_TIMEOUT(HttpStatus.BAD_GATEWAY, "G005", "요청 시간이 초과되었습니다."),

    // Image (Day 7)
    IMAGE_PROMPT_REQUIRED(HttpStatus.BAD_REQUEST, "I001", "이미지 생성을 위한 프롬프트를 입력해 주세요."),
    IMAGE_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "I002", "오늘의 이미지 생성 횟수 한도를 초과했습니다."),
    IMAGE_DOWNLOAD_FAILED(HttpStatus.BAD_GATEWAY, "I003", "생성된 이미지를 가져오지 못했습니다."),
    IMAGE_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "I004", "이미지를 저장하는 중 문제가 발생했습니다."),
    IMAGE_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "I005", "이미지 생성에 실패했습니다."),

    // Vision (Day 8)
    VISION_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "V001", "이미지 파일을 첨부해 주세요."),
    VISION_INVALID_MIME_TYPE(HttpStatus.BAD_REQUEST, "V002", "지원하지 않는 이미지 형식입니다. (png, jpg, gif, webp)"),
    VISION_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "V003", "이미지 업로드에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
