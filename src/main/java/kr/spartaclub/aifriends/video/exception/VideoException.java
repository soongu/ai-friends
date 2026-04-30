package kr.spartaclub.aifriends.video.exception;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;

/**
 * Day 10 — Video (비디오 생성 선택 실습) 도메인 커스텀 예외.
 *
 * <p>비디오 생성은 학생 선택 실습이므로 실제 외부 호출은 일어나지 않고
 * Stub 폴링 클라이언트가 응답을 흉내낸다. 그래도 입력 검증 / 모르는 jobId / 예산 초과 같은
 * 결은 모두 이 예외로 던져 {@code GlobalExceptionHandler} 가 ApiResponse.fail 로 자동 변환한다.</p>
 */
public class VideoException extends BusinessException {

    public VideoException(ErrorCode errorCode) {
        super(errorCode);
    }
}
