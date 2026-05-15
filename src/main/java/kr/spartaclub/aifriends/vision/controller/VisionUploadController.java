package kr.spartaclub.aifriends.vision.controller;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.common.response.ApiResponse;
import kr.spartaclub.aifriends.image.service.ImageFileStorageService;
import kr.spartaclub.aifriends.vision.dto.VisionUploadResponse;
import kr.spartaclub.aifriends.vision.exception.VisionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

/**
 * Day 8 Step 5 — 사용자 이미지 업로드 엔드포인트.
 *
 * <p>{@code multipart/form-data} 로 받은 이미지 파일을 검증한 뒤 로컬 파일시스템에 저장하고,
 * 정적 리소스 경로를 응답으로 돌려준다. Step 6 에서는 이 {@code publicPath} 를 그대로
 * {@code VisionChatService.describe(...)} 의 입력 URL 로 흘려보내 멀티모달 분석을 이어간다.</p>
 *
 * <p>응답은 {@link ApiResponse} 로 래핑한다 (§4-1 게이트). 검증 실패는
 * {@link VisionException} 으로만 던지고 {@code GlobalExceptionHandler} 가 표준 에러 응답으로 변환한다.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/vision")
public class VisionUploadController {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp"
    );

    private final ImageFileStorageService imageFileStorageService;

    public VisionUploadController(ImageFileStorageService imageFileStorageService) {
        this.imageFileStorageService = imageFileStorageService;
    }

    @PostMapping("/uploads")
    public ResponseEntity<ApiResponse<VisionUploadResponse>> upload(
            @RequestParam("image") MultipartFile image) {

        if (image == null || image.isEmpty()) {
            throw new VisionException(ErrorCode.VISION_IMAGE_REQUIRED);
        }

        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new VisionException(ErrorCode.VISION_INVALID_MIME_TYPE);
        }

        String extension = mapExtension(contentType);

        try {
            String publicPath = imageFileStorageService.save(image.getBytes(), "upload", extension);
            VisionUploadResponse response = new VisionUploadResponse(
                    publicPath,
                    contentType,
                    image.getSize()
            );
            log.info("[VisionUpload] saved: contentType={}, size={}, publicPath={}",
                    contentType, image.getSize(), publicPath);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IOException e) {
            log.error("[VisionUpload] storage failed", e);
            throw new VisionException(ErrorCode.VISION_UPLOAD_FAILED);
        }
    }

    /**
     * contentType → 파일 확장자 단순 매핑. 학습용 단순도 유지를 위해 if-else 로 박는다.
     */
    private String mapExtension(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}
