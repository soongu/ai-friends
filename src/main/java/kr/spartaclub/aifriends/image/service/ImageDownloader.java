package kr.spartaclub.aifriends.image.service;

/**
 * Day 7 Step 4 — 외부 이미지 URL 을 바이트 배열로 가져오는 단일 책임 추상.
 *
 * <p>{@link ImageGenerationService} 가 RestClient 를 직접 받지 않고 이 인터페이스에 의존하면,
 * 호출 의도(=다운로드) 가 명확해지고, 프록시/캐시/CDN 같은 변형 구현체로 갈아끼우는 비용이 사라진다.
 * §4 추상화 정신 — 프로바이더가 어떻게 가져오느냐는 호출자가 알 필요 없다.</p>
 */
public interface ImageDownloader {

    /**
     * @throws ImageDownloadException 외부 호출 실패 (HTTP 4xx/5xx, 타임아웃, 연결 오류 등)
     */
    byte[] download(String url);

    /**
     * 다운로더 내부의 모든 외부 호출 실패를 한 곳으로 모은다.
     * 호출자({@link ImageGenerationService}) 는 이 예외만 잡아서 도메인 예외로 래핑한다.
     */
    class ImageDownloadException extends RuntimeException {
        public ImageDownloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
