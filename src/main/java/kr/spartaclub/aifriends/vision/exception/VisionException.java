package kr.spartaclub.aifriends.vision.exception;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;

/**
 * Day 8 Step 5 — Vision (멀티모달 입력) 도메인 커스텀 예외.
 *
 * <p>{@code IllegalArgumentException} / {@code RuntimeException} 직접 throw 금지 규칙을 따라,
 * 사용자 이미지 업로드 검증 실패 · 저장 실패 · MIME 타입 거절 등은 모두 이 예외로 래핑한다.
 * {@link ErrorCode} 의 {@code VISION_*} 항목과 함께 던지면 {@code GlobalExceptionHandler}
 * 가 ApiResponse.fail 형태로 자동 변환해준다.</p>
 */
public class VisionException extends BusinessException {

    public VisionException(ErrorCode errorCode) {
        super(errorCode);
    }
}
