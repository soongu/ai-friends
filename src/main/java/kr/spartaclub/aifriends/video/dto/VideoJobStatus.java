package kr.spartaclub.aifriends.video.dto;

/**
 * Day 10 — 비동기 비디오 생성 Job 의 상태.
 *
 * <p>Day 6 SSE (스트리밍) · Day 9 binary (음성) 에 이은 *세 번째 응답 패턴* — 비동기 폴링의 결.
 * 클라이언트는 {@code submit} 으로 Job 을 큐에 넣고, {@code GET /status/{jobId}} 를 주기적으로
 * 호출해 상태를 확인한다. 완료까지 보통 *분 단위* 가 걸리는 자리다.</p>
 *
 * <ul>
 *   <li>{@link #QUEUED} — 큐에 등록만 됐고 워커가 아직 집어가지 않은 상태</li>
 *   <li>{@link #RUNNING} — 워커가 집어가 모델 추론 중</li>
 *   <li>{@link #SUCCEEDED} — 완료, {@code videoUrl} 이 함께 내려옴</li>
 *   <li>{@link #FAILED} — 실패, {@code errorMessage} 가 함께 내려옴</li>
 * </ul>
 */
public enum VideoJobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED
}
