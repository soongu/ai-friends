package kr.spartaclub.aifriends.vision.dto;

/**
 * Day 8 Step 5 — 사용자 이미지 업로드 응답 DTO.
 *
 * @param publicPath   우리 서버의 정적 리소스 경로 (예: {@code /uploads/portraits/upload-xxx.png}).
 *                     Step 6 에서 {@code VisionChatService} 의 입력 URL 로 흘려보낼 자리.
 * @param contentType  원본 multipart 의 contentType (예: {@code image/png}).
 * @param sizeBytes    저장된 이미지 크기 (바이트).
 */
public record VisionUploadResponse(
        String publicPath,
        String contentType,
        long sizeBytes
) {
}
