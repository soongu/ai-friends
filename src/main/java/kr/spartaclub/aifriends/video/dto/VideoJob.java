package kr.spartaclub.aifriends.video.dto;

/**
 * Day 10 — 비동기 비디오 생성 Job 의 스냅샷.
 *
 * <p>{@code submit(...)} 시점엔 {@code status=QUEUED} 와 {@code jobId} 만 채워지고,
 * 시간이 지나 폴링하면 {@code status} 가 {@code RUNNING → SUCCEEDED/FAILED} 로 흘러간다.
 * {@code SUCCEEDED} 일 때만 {@code videoUrl} 이 채워지고, {@code FAILED} 일 때만
 * {@code errorMessage} 가 채워진다 — 비어 있는 필드는 {@code null} 로 둔다.</p>
 *
 * @param jobId        Job UUID — submit 시 서버가 부여
 * @param status       현재 상태
 * @param videoUrl     성공 시 다운로드 가능한 URL (이외에는 null)
 * @param errorMessage 실패 시 사람이 읽을 메시지 (이외에는 null)
 */
public record VideoJob(
        String jobId,
        VideoJobStatus status,
        String videoUrl,
        String errorMessage) {

    /** QUEUED 상태로 새 Job 생성 — submit 시점에 사용. */
    public static VideoJob queued(String jobId) {
        return new VideoJob(jobId, VideoJobStatus.QUEUED, null, null);
    }
}
