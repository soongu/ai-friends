package kr.spartaclub.aifriends.image.exception;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;

/**
 * Day 7 Step 4~5 — 이미지 도메인 커스텀 예외.
 *
 * <p>{@code IllegalArgumentException} / {@code RuntimeException} 직접 throw 금지 규칙
 * (code-tdd-verifier 게이트) 을 따라, 이미지 생성·다운로드·저장 실패는 모두 이 예외로 래핑한다.
 * {@link ErrorCode} 의 {@code IMAGE_*} 항목과 함께 던지면 {@code GlobalExceptionHandler}
 * 가 ApiResponse.fail 형태로 자동 변환해준다.</p>
 */
public class ImageException extends BusinessException {

    public ImageException(ErrorCode errorCode) {
        super(errorCode);
    }
}
