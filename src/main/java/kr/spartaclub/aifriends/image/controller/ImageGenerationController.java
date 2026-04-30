package kr.spartaclub.aifriends.image.controller;

import jakarta.validation.Valid;
import kr.spartaclub.aifriends.common.response.ApiResponse;
import kr.spartaclub.aifriends.image.dto.ImageGenerationResult;
import kr.spartaclub.aifriends.image.dto.PortraitGenerationRequest;
import kr.spartaclub.aifriends.image.service.ImageGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Day 7 Step 6 — 캐릭터 초상화 생성 엔드포인트.
 *
 * <p>{@link ApiResponse} 로 정상 응답을 래핑한다 (§4-1 게이트). 에러 응답은 GlobalExceptionHandler 가
 * {@code ApiResponse.fail(...)} 로 자동 변환하므로, 정상/에러 응답 형태가 대칭으로 맞춰진다.</p>
 */
@RestController
@RequestMapping("/api/images")
public class ImageGenerationController {

    private final ImageGenerationService imageGenerationService;

    public ImageGenerationController(ImageGenerationService imageGenerationService) {
        this.imageGenerationService = imageGenerationService;
    }

    @PostMapping("/portraits")
    public ResponseEntity<ApiResponse<ImageGenerationResult>> generatePortrait(
            @Valid @RequestBody PortraitGenerationRequest request) {
        String fileNameHint = "portrait-" + UUID.randomUUID();
        ImageGenerationResult result = imageGenerationService.generate(
                request.prompt(),
                request.stylePreset(),
                request.seed(),
                fileNameHint);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
