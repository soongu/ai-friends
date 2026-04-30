package kr.spartaclub.aifriends.voice.exception;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;

/**
 * Day 9 Step 3 — Voice (STT/TTS) 도메인 커스텀 예외.
 *
 * <p>{@code IllegalArgumentException} / {@code RuntimeException} 직접 throw 금지 규칙을 따라,
 * 음성 파일 업로드 검증 실패 · STT 호출 실패 · TTS 합성 실패 등은 모두 이 예외로 래핑한다.
 * {@link ErrorCode} 의 {@code VOICE_*} 항목과 함께 던지면 {@code GlobalExceptionHandler}
 * 가 ApiResponse.fail 형태로 자동 변환해준다.</p>
 */
public class VoiceException extends BusinessException {

    public VoiceException(ErrorCode errorCode) {
        super(errorCode);
    }
}
