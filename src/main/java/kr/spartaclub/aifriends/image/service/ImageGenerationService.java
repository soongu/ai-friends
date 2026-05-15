package kr.spartaclub.aifriends.image.service;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.image.dto.ImageGenerationResult;
import kr.spartaclub.aifriends.image.exception.ImageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Value;
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

    private final ImageModel imageModel;
    private final ImageDownloader imageDownloader;
    private final ImageDailyQuotaGuard quotaGuard;
    private final ImageCostEstimator costEstimator;
    private final ImageFileStorageService storageService;

    /** 비용 매핑 키 + 응답 DTO 의 modelName 필드. application.yml 의 aifriends.image.model-name 으로 토글. */
    private final String modelName;
    /** pollinations | openai. 옵션 객체(PollinationsImageOptions)를 넘길지 분기. */
    private final String provider;

    public ImageGenerationService(ImageModel imageModel,
                                  ImageDownloader imageDownloader,
                                  ImageDailyQuotaGuard quotaGuard,
                                  ImageCostEstimator costEstimator,
                                  ImageFileStorageService storageService,
                                  @Value("${aifriends.image.model-name:pollinations-flux}") String modelName,
                                  @Value("${aifriends.image.provider:pollinations}") String provider) {
        this.imageModel = imageModel;
        this.imageDownloader = imageDownloader;
        this.quotaGuard = quotaGuard;
        this.costEstimator = costEstimator;
        this.storageService = storageService;
        this.modelName = modelName;
        this.provider = provider;
    }

    public ImageGenerationResult generate(String prompt, String stylePreset, Long seed, String fileNameHint) {
        // (1) 한도 체크 — 한도 초과면 ImageModel 호출조차 일어나지 않는다 (비용 차단의 본질).
        quotaGuard.checkAndIncrement();

        // (2) 비용 에코 — 학습용. 운영에선 Prometheus counter / cost-tracker 로 대체.
        costEstimator.echo(modelName);

        // (3) ImageModel 호출 — 프로바이더 추상화의 핵심. 우리 코드는 ImageModel 인터페이스만 안다.
        // ai-friends 는 미연시 게임이라 *항상 애니풍 일러스트* 를 강제. DALL-E 3 가 짧은 prompt 를
        // photorealistic 으로 자동 expansion 하는 경향을 키워드로 락한다.
        String enrichedPrompt = ensureAnimeStyle(
                (stylePreset == null || stylePreset.isBlank())
                        ? prompt
                        : prompt + ", style: " + stylePreset);
        // 프로바이더별 옵션 분기:
        //   - pollinations: PollinationsImageOptions (model="flux" + style + seed 등 자체 키)
        //   - openai: 빈 OpenAiImageOptions. OpenAiImageModel.call() 이 호출 옵션을 (OpenAiImageOptions) 로
        //     무조건 cast 하므로, 공통 ImageOptions 인터페이스 구현체나 null 을 넘기면 ClassCastException /
        //     IllegalArgumentException 으로 막힌다. 빈 빌더 결과는 *모든 필드 null* 이므로 OpenAiImageModel
        //     빈 등록 시 박은 *defaults* (model/quality/style/size) 와 자동 병합되어 그대로 적용된다.
        ImageOptions options = isPollinations()
                ? new PollinationsImageOptions("flux", 1024, 1024, seed, stylePreset)
                : OpenAiImageOptions.builder().build();
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

        double estimatedCost = costEstimator.estimateCostUsd(modelName);
        return new ImageGenerationResult(localPath, externalUrl, enrichedPrompt, modelName, estimatedCost);
    }

    private boolean isPollinations() {
        return "pollinations".equalsIgnoreCase(provider);
    }

    /**
     * 미연시 게임 정체성 — *애니풍 일러스트* 강제.
     *
     * <p>DALL-E 3 는 짧은 prompt 를 자체 prompt-expansion 으로 photorealistic 결로 자동 변환하는 경향이 있다.
     * 사용자 커스텀 prompt 가 *"단발머리에 안경 쓴 차분한 여성"* 처럼 짧을 때 실사 결과가 도착하는 자리.
     * 모든 호출에 *anime portrait illustration, soft cel shading, korean character, no photorealism*
     * suffix 를 자동 박아 스타일을 락한다. 이미 appearancePrompt (CharacterPreset enum 메타) 에 anime
     * 키워드가 들어있는 셀카 요청 같은 자리는 *중복 박혀도 모델이 자연스럽게 흡수* 한다.</p>
     */
    private String ensureAnimeStyle(String prompt) {
        if (prompt == null) return null;
        String lower = prompt.toLowerCase(java.util.Locale.ROOT);
        boolean alreadyAnime = lower.contains("anime") || lower.contains("illustration") || lower.contains("cel shading");
        if (alreadyAnime) return prompt;
        return prompt + ", anime portrait illustration, soft cel shading, korean character, no photorealism";
    }
}
