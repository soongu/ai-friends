package kr.spartaclub.aifriends.image.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ImageFileStorageServiceTest {

    @Test
    @DisplayName("바이트 배열을 업로드 디렉토리에 저장하고 /uploads/portraits/... 형태의 경로를 돌려준다")
    void should_save_bytes_to_upload_dir(@TempDir Path tempDir) throws IOException {
        ImageFileStorageService sut = new ImageFileStorageService(tempDir.toString());
        byte[] bytes = "fake-image-bytes".getBytes();

        String publicPath = sut.save(bytes, "portrait-test");

        assertThat(publicPath).startsWith("/uploads/portraits/portrait-test-");
        assertThat(publicPath).endsWith(".jpg");

        // 실제 파일이 디스크에 생겼는지 검증
        String fileName = publicPath.substring("/uploads/portraits/".length());
        Path actualFile = tempDir.resolve(fileName);
        assertThat(Files.exists(actualFile)).isTrue();
        assertThat(Files.readAllBytes(actualFile)).isEqualTo(bytes);
    }

    @Test
    @DisplayName("업로드 디렉토리가 없으면 자동으로 생성한다")
    void should_create_directory_when_not_exists(@TempDir Path tempDir) throws IOException {
        Path nestedDir = tempDir.resolve("not-yet-exists/portraits");
        ImageFileStorageService sut = new ImageFileStorageService(nestedDir.toString());

        String publicPath = sut.save("x".getBytes(), "abc");

        assertThat(Files.isDirectory(nestedDir)).isTrue();
        assertThat(publicPath).contains("abc");
    }

    @Test
    @DisplayName("파일명 힌트의 위험 문자(/, .., 공백 등)는 sanitize 된다")
    void should_sanitize_unsafe_filename_hint(@TempDir Path tempDir) throws IOException {
        ImageFileStorageService sut = new ImageFileStorageService(tempDir.toString());

        String publicPath = sut.save("x".getBytes(), "../etc/passwd portrait");

        assertThat(publicPath).doesNotContain("..");
        assertThat(publicPath).doesNotContain("/etc/");
        assertThat(publicPath).doesNotContain(" ");
    }

    @Test
    @DisplayName("Day 8 Step 5 — extension=png 으로 저장하면 publicPath 가 .png 로 끝난다")
    void should_use_png_extension_when_specified(@TempDir Path tempDir) throws IOException {
        ImageFileStorageService sut = new ImageFileStorageService(tempDir.toString());

        String publicPath = sut.save("x".getBytes(), "upload", "png");

        assertThat(publicPath).startsWith("/uploads/portraits/upload-");
        assertThat(publicPath).endsWith(".png");
    }

    @Test
    @DisplayName("Day 8 Step 5 — extension 이 null 또는 빈 문자열이면 .jpg 로 폴백한다")
    void should_fallback_to_jpg_when_extension_blank(@TempDir Path tempDir) throws IOException {
        ImageFileStorageService sut = new ImageFileStorageService(tempDir.toString());

        String nullExtPath = sut.save("x".getBytes(), "upload", null);
        String blankExtPath = sut.save("x".getBytes(), "upload", "  ");
        String invalidExtPath = sut.save("x".getBytes(), "upload", "p!ng");

        assertThat(nullExtPath).endsWith(".jpg");
        assertThat(blankExtPath).endsWith(".jpg");
        assertThat(invalidExtPath).endsWith(".jpg");
    }
}
