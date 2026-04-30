package kr.spartaclub.aifriends.image.service;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.image.dto.ImageGenerationResult;
import kr.spartaclub.aifriends.image.exception.ImageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageGenerationServiceTest {

    @Mock ImageModel imageModel;
    @Mock ImageDownloader imageDownloader;
    @Mock ImageDailyQuotaGuard quotaGuard;
    @Mock ImageCostEstimator costEstimator;
    @Mock ImageFileStorageService storageService;

    private ImageGenerationService sut() {
        return new ImageGenerationService(imageModel, imageDownloader, quotaGuard, costEstimator, storageService);
    }

    @Test
    @DisplayName("성공 경로: ImageModel 호출 → 다운로드 → 저장 → 결과 DTO 반환")
    void should_return_result_with_local_path_when_generate_success() throws IOException {
        // given
        String externalUrl = "https://image.pollinations.ai/prompt/test?model=flux";
        byte[] bytes = "fake".getBytes();
        String localPath = "/uploads/portraits/portrait-abc-123.jpg";

        ImageResponse fakeResponse = new ImageResponse(List.of(new ImageGeneration(new Image(externalUrl, null))));
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(fakeResponse);
        when(imageDownloader.download(externalUrl)).thenReturn(bytes);
        when(storageService.save(bytes, "portrait-abc")).thenReturn(localPath);
        when(costEstimator.estimateCostUsd("pollinations-flux")).thenReturn(0.0);

        // when
        ImageGenerationResult result = sut().generate("a cat", null, null, "portrait-abc");

        // then
        assertThat(result.localPath()).isEqualTo(localPath);
        assertThat(result.externalUrl()).isEqualTo(externalUrl);
        assertThat(result.modelName()).isEqualTo("pollinations-flux");
        assertThat(result.estimatedCostUsd()).isEqualTo(0.0);
        assertThat(result.prompt()).isEqualTo("a cat");
    }

    @Test
    @DisplayName("일일 한도 초과 시 ImageModel 호출 자체가 일어나지 않는다 (비용 차단)")
    void should_throw_image_exception_when_quota_exceeded() {
        // given
        doThrow(new ImageException(ErrorCode.IMAGE_QUOTA_EXCEEDED))
                .when(quotaGuard).checkAndIncrement();

        // when / then
        assertThatThrownBy(() -> sut().generate("anything", null, null, "x"))
                .isInstanceOf(ImageException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_QUOTA_EXCEEDED);

        verify(imageModel, never()).call(any(ImagePrompt.class));
        verify(imageDownloader, never()).download(anyString());
    }

    @Test
    @DisplayName("RestClient 다운로드 실패는 IMAGE_DOWNLOAD_FAILED 로 래핑된다")
    void should_throw_image_download_failed_when_downloader_throws() {
        ImageResponse fakeResponse = new ImageResponse(List.of(new ImageGeneration(new Image("http://x", null))));
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(fakeResponse);
        when(imageDownloader.download(anyString()))
                .thenThrow(new ImageDownloader.ImageDownloadException("boom", new RuntimeException()));

        assertThatThrownBy(() -> sut().generate("a cat", null, null, "x"))
                .isInstanceOf(ImageException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_DOWNLOAD_FAILED);
    }

    @Test
    @DisplayName("스토리지 저장 IOException 은 IMAGE_STORAGE_FAILED 로 래핑된다")
    void should_throw_image_storage_failed_when_storage_throws() throws IOException {
        ImageResponse fakeResponse = new ImageResponse(List.of(new ImageGeneration(new Image("http://x", null))));
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(fakeResponse);
        when(imageDownloader.download(anyString())).thenReturn("ok".getBytes());
        when(storageService.save(any(), anyString())).thenThrow(new IOException("disk full"));

        assertThatThrownBy(() -> sut().generate("a cat", null, null, "x"))
                .isInstanceOf(ImageException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_STORAGE_FAILED);
    }

    @Test
    @DisplayName("호출 순서 보장: quota → cost echo → imageModel.call → download → save")
    void should_call_in_order_quota_cost_image_download_save() throws IOException {
        ImageResponse fakeResponse = new ImageResponse(List.of(new ImageGeneration(new Image("http://x", null))));
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(fakeResponse);
        when(imageDownloader.download(anyString())).thenReturn("ok".getBytes());
        when(storageService.save(any(), anyString())).thenReturn("/uploads/portraits/x.jpg");

        sut().generate("a cat", null, null, "x");

        InOrder inOrder = inOrder(quotaGuard, costEstimator, imageModel, imageDownloader, storageService);
        inOrder.verify(quotaGuard).checkAndIncrement();
        inOrder.verify(costEstimator).echo("pollinations-flux");
        inOrder.verify(imageModel).call(any(ImagePrompt.class));
        inOrder.verify(imageDownloader).download(anyString());
        inOrder.verify(storageService).save(any(), anyString());
    }
}
