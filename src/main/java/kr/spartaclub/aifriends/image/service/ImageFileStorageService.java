package kr.spartaclub.aifriends.image.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Day 7 Step 7 — 생성된 이미지를 로컬 파일시스템에 저장하고 정적 리소스 URL 을 돌려준다.
 *
 * <p>업로드 디렉토리는 {@code aifriends.image.storage.upload-dir} (기본값 {@code ./uploads/portraits}).
 * {@code StaticResourceConfig} 가 {@code /uploads/**} 를 {@code file:./uploads/} 로 매핑하므로
 * 반환된 경로(예: {@code /uploads/portraits/portrait-xxx.jpg}) 는 브라우저에서 바로 조회된다.</p>
 *
 * <p>실제 운영에서는 S3 / CloudFront / GCS 등 객체 스토리지로 대체하는 것이 정석이다.
 * 학습용 로컬 저장 시나리오로는 충분하지만, 다음 한계를 학생에게 명시해야 한다:
 * <ul>
 *   <li>WAS 인스턴스가 N대면 파일이 한 인스턴스에만 떨어진다 → 객체 스토리지 필수</li>
 *   <li>컨테이너 재기동 시 휘발 → 볼륨 마운트 또는 외부 스토리지</li>
 *   <li>디스크 용량 폭발 위험 → TTL/cleanup 정책 필요</li>
 * </ul></p>
 */
@Slf4j
@Service
public class ImageFileStorageService {

    private final String uploadDir;

    public ImageFileStorageService(
            @Value("${aifriends.image.storage.upload-dir:./uploads/portraits}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    /**
     * 바이트 배열을 파일로 저장하고 정적 리소스 URL 을 돌려준다 (확장자 jpg 고정).
     *
     * <p>Day 7 의 호출부 호환을 위해 시그니처를 유지한다. 내부적으로
     * {@link #save(byte[], String, String)} 에 {@code "jpg"} 를 넘겨 위임한다.</p>
     *
     * @param bytes        이미지 바이트
     * @param fileNameHint 파일명 힌트 (예: {@code portrait-uuid}). 안전한 문자만 남기고 sanitize 한다.
     * @return 정적 리소스 URL (예: {@code /uploads/portraits/portrait-xxx-1714500000000.jpg})
     * @throws IOException 디렉토리 생성 또는 파일 쓰기 실패 시
     */
    public String save(byte[] bytes, String fileNameHint) throws IOException {
        return save(bytes, fileNameHint, "jpg");
    }

    /**
     * Day 8 Step 5 — 사용자 업로드용. 임의 확장자(png · jpg · gif · webp 등) 를 받아 저장한다.
     *
     * @param bytes        이미지 바이트
     * @param fileNameHint 파일명 힌트 (예: {@code upload}). 안전한 문자만 남기고 sanitize 한다.
     * @param extension    확장자 (예: {@code "png"}). 영문자만 허용, 소문자 정규화, null/blank 면 {@code "jpg"} 폴백.
     * @return 정적 리소스 URL (예: {@code /uploads/portraits/upload-xxx-1714500000000.png})
     * @throws IOException 디렉토리 생성 또는 파일 쓰기 실패 시
     */
    public String save(byte[] bytes, String fileNameHint, String extension) throws IOException {
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);

        String safeHint = sanitize(fileNameHint);
        String safeExt = sanitizeExtension(extension);
        String fileName = safeHint + "-" + System.currentTimeMillis() + "." + safeExt;
        Path target = dir.resolve(fileName);
        Files.write(target, bytes);

        String publicPath = "/uploads/portraits/" + fileName;
        log.info("[ImageFileStorage] saved: bytes={}, path={}", bytes.length, target.toAbsolutePath());
        return publicPath;
    }

    private String sanitize(String hint) {
        if (hint == null || hint.isBlank()) {
            return "image";
        }
        return hint.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String sanitizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "jpg";
        }
        String lower = extension.toLowerCase();
        if (!lower.matches("[a-z]+")) {
            return "jpg";
        }
        return lower;
    }
}
