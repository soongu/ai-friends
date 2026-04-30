package kr.spartaclub.aifriends.image.service;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.image.dto.ImageGenerationResult;
import kr.spartaclub.aifriends.image.exception.ImageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Day 7 Step 4 — 이미지 생성의 도메인 진입점.
 *
 * <p>책임 분리:
 * <ol>
 *   <li>{@link ImageDailyQuotaGuard}: 호출 전 한도 체크</li>
 *   <li>{@link ImageCostEstimator}: 호출 전 비용 로그 에코</li>
 *   <li>{@link ImageModel}: 프롬프트 → 외부 URL (어떤 프로바이더든)</li>
 *   <li>{@link ImageDownloader}: 외부 URL → 바이트</li>
 *   <li>{@link ImageFileStorageService}: 바이트 → 우리 서버 정적 리소스 경로</li>
 * </ol>
 *
 * <p>{@link ImageModel} 은 인터페이스로만 주입받는다 (§4 게이트). 빈은 {@code pollinationsImageModel}
 * 이지만 호출자는 모른다 — {@code application.yml} 의 빈 교체만으로 OpenAI / Stability / Imagen 으로
 * 갈아끼울 수 있다는 게 이 추상화의 핵심.</p>
 *
 * <p>모든 실패 경로는 {@link ImageException} + 도메인 {@link ErrorCode} 로 래핑한다.
 * {@link RuntimeException} / {@link IllegalArgumentException} 직접 throw 금지 (TDD 검증 게이트).</p>
 */
@Slf4j
@Service
public class ImageGenerationService {

    private static final String MODEL_NAME = "pollinations-flux";

    private final ImageModel imageModel;
    private final ImageDownloader imageDownloader;
    private final ImageDailyQuotaGuard quotaGuard;
    private final ImageCostEstimator costEstimator;
    private final ImageFileStorageService storageService;

    public ImageGenerationService(ImageModel imageModel,
                                  ImageDownloader imageDownloader,
                                  ImageDailyQuotaGuard quotaGuard,
                                  ImageCostEstimator costEstimator,
                                  ImageFileStorageService storageService) {
        this.imageModel = imageModel;
        this.imageDownloader = imageDownloader;
        this.quotaGuard = quotaGuard;
        this.costEstimator = costEstimator;
        this.storageService = storageService;
    }

    public ImageGenerationResult generate(String prompt, String stylePreset, Long seed, String fileNameHint) {
        // (1) 한도 체크 — 한도 초과면 ImageModel 호출조차 일어나지 않는다 (비용 차단의 본질).
        quotaGuard.checkAndIncrement();

        // (2) 비용 에코 — 학습용. 운영에선 Prometheus counter / cost-tracker 로 대체.
        costEstimator.echo(MODEL_NAME);

        // (3) ImageModel 호출 — 프로바이더 추상화의 핵심. 우리 코드는 ImageModel 인터페이스만 안다.
        String enrichedPrompt = (stylePreset == null || stylePreset.isBlank())
                ? prompt
                : prompt + ", style: " + stylePreset;
        PollinationsImageOptions options = new PollinationsImageOptions(
                "flux", 1024, 1024, seed, stylePreset);
        ImageResponse response;
        try {
            response = imageModel.call(new ImagePrompt(enrichedPrompt, options));
        } catch (RuntimeException e) {
            log.warn("[ImageGeneration] provider call failed: {}", e.getMessage(), e);
            throw new ImageException(ErrorCode.IMAGE_GENERATION_FAILED);
        }
        String externalUrl = response.getResult().getOutput().getUrl();

        // (4) 외부 이미지 다운로드 — RestClient 추상화는 ImageDownloader 가 흡수.
        byte[] bytes;
        try {
            bytes = imageDownloader.download(externalUrl);
        } catch (ImageDownloader.ImageDownloadException e) {
            log.warn("[ImageGeneration] download failed: {}", e.getMessage(), e);
            throw new ImageException(ErrorCode.IMAGE_DOWNLOAD_FAILED);
        }

        // (5) 로컬 저장 + 정적 리소스 경로 반환.
        String localPath;
        try {
            localPath = storageService.save(bytes, fileNameHint);
        } catch (IOException e) {
            log.error("[ImageGeneration] storage failed: {}", e.getMessage(), e);
            throw new ImageException(ErrorCode.IMAGE_STORAGE_FAILED);
        }

        double estimatedCost = costEstimator.estimateCostUsd(MODEL_NAME);
        return new ImageGenerationResult(localPath, externalUrl, enrichedPrompt, MODEL_NAME, estimatedCost);
    }
}
