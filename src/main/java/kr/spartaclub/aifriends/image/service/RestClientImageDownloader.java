package kr.spartaclub.aifriends.image.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

/**
 * Day 7 Step 4 — {@link ImageDownloader} 의 RestClient 기반 프로덕션 구현.
 *
 * <p>외부 이미지 URL 을 바이트 배열로 가져오고, 모든 RestClient 예외를 {@link ImageDownloadException}
 * 으로 단일화한다. 서비스 계층은 이 단일 예외만 잡아 {@code ImageException(IMAGE_DOWNLOAD_FAILED)}
 * 로 래핑하면 된다.</p>
 *
 * <p><b>Azure SAS URL 의 이중 인코딩 함정</b>: OpenAI DALL-E 응답 URL 은 Azure Blob Storage 의
 * SAS (Shared Access Signature) URL 이라 query string 에 {@code sig=...%3D} 같은 *이미 인코딩된* 값이
 * 박혀 있다. {@code RestClient.uri(String)} 은 URI template 으로 받아 다시 인코딩하므로 {@code %3D}
 * 가 {@code %253D} 로 이중 인코딩되며 Azure 가 signature 검증을 실패시켜 403 을 떨군다. {@link URI#create}
 * 로 *완성된 URI 객체* 를 넘기면 RestClient 가 더 이상 손대지 않는다.</p>
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
                    .uri(URI.create(url))   // String 이 아니라 완성된 URI — 재인코딩 차단
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
