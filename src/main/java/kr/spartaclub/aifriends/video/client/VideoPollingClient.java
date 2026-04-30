package kr.spartaclub.aifriends.video.client;

import kr.spartaclub.aifriends.video.dto.VideoGenerationRequest;
import kr.spartaclub.aifriends.video.dto.VideoJob;

/**
 * Day 10 — 비디오 생성 비동기 폴링 클라이언트의 *경계 인터페이스*.
 *
 * <p>비디오 생성은 *한 번의 동기 호출로 떨어지지 않는다.* {@code submit} 으로 Job 을 큐에 던지면
 * 서버 측에서 분 단위로 추론이 돌아가고, 클라이언트는 {@code pollStatus} 를 주기적으로 호출해
 * 상태를 확인한다 — *Day 6 SSE 도 Day 9 binary 도 아닌* 세 번째 응답 패턴.</p>
 *
 * <p>이 인터페이스는 *프로바이더 추상화* 의 결을 따라 박혀 있다. 학생 실습은
 * {@link StubVideoPollingClient} (인메모리 + 시간 기반 시뮬레이션) 로 진행하고, 실제 Veo 3 ·
 * Sora · Runway 어댑터는 같은 인터페이스 뒤에서 갈아끼울 수 있도록 *경계만* 박아둔다.</p>
 */
public interface VideoPollingClient {

    /**
     * Job 을 큐에 등록하고 {@code QUEUED} 상태의 스냅샷을 즉시 돌려준다.
     *
     * @param request 비디오 생성 요청
     * @return jobId 가 채워진 {@link VideoJob} (status = QUEUED)
     */
    VideoJob submit(VideoGenerationRequest request);

    /**
     * 주어진 {@code jobId} 의 현재 상태 스냅샷을 돌려준다.
     *
     * @param jobId submit 시 부여된 식별자
     * @return 현재 시점의 {@link VideoJob} 스냅샷
     * @throws kr.spartaclub.aifriends.video.exception.VideoException
     *         존재하지 않는 jobId 인 경우 {@code VIDEO_JOB_NOT_FOUND}
     */
    VideoJob pollStatus(String jobId);
}
