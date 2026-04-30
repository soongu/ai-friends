package kr.spartaclub.aifriends.image.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Day 7 Step 4 — {@link ImageDownloader} 의 RestClient 기반 프로덕션 구현.
 *
 * <p>외부 이미지 URL 을 바이트 배열로 가져오고, 모든 RestClient 예외를 {@link ImageDownloadException}
 * 으로 단일화한다. 서비스 계층은 이 단일 예외만 잡아 {@code ImageException(IMAGE_DOWNLOAD_FAILED)}
 * 로 래핑하면 된다.</p>
 */
@Component
public class RestClientImageDownloader implements ImageDownloader {

    private final RestClient externalImageRestClient;

    public RestClientImageDownloader(@Qualifier("externalImageRestClient") RestClient externalImageRestClient) {
        this.externalImageRestClient = externalImageRestClient;
    }

    @Override
    public byte[] download(String url) {
        try {
            byte[] bytes = externalImageRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new ImageDownloadException("Empty image body: " + url, null);
            }
            return bytes;
        } catch (RestClientException e) {
            throw new ImageDownloadException("Failed to download image: " + url, e);
        }
    }
}
