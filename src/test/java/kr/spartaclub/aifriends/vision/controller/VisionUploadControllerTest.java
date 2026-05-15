package kr.spartaclub.aifriends.vision.controller;

import kr.spartaclub.aifriends.common.exception.GlobalExceptionHandler;
import kr.spartaclub.aifriends.image.service.ImageFileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VisionUploadController.class)
@Import(GlobalExceptionHandler.class)
class VisionUploadControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ImageFileStorageService imageFileStorageService;

    @Test
    @DisplayName("정상 png 업로드는 200 + ApiResponse.success + publicPath 가 .png 로 끝난다")
    void should_return_200_with_png_publicPath_when_valid_png_upload() throws Exception {
        when(imageFileStorageService.save(any(byte[].class), eq("upload"), eq("png")))
                .thenReturn("/uploads/portraits/upload-xxx-1714500000000.png");

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "cat.png",
                "image/png",
                "fake-png-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/vision/uploads").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicPath", startsWith("/uploads/portraits/upload-")))
                .andExpect(jsonPath("$.data.publicPath", endsWith(".png")))
                .andExpect(jsonPath("$.data.contentType").value("image/png"));
    }

    @Test
    @DisplayName("정상 jpg 업로드는 publicPath 가 .jpg 로 끝난다")
    void should_return_200_with_jpg_publicPath_when_valid_jpeg_upload() throws Exception {
        when(imageFileStorageService.save(any(byte[].class), eq("upload"), eq("jpg")))
                .thenReturn("/uploads/portraits/upload-xxx-1714500000000.jpg");

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "cat.jpg",
                "image/jpeg",
                "fake-jpg-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/vision/uploads").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicPath", endsWith(".jpg")))
                .andExpect(jsonPath("$.data.contentType").value("image/jpeg"));
    }

    @Test
    @DisplayName("빈 파일은 400 + V001 으로 응답한다")
    void should_return_400_when_file_is_empty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "image",
                "empty.png",
                "image/png",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/vision/uploads").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("V001"));
    }

    @Test
    @DisplayName("지원하지 않는 contentType 은 400 + V002 로 응답한다")
    void should_return_400_when_unsupported_content_type() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "image",
                "note.txt",
                "text/plain",
                "hello".getBytes()
        );

        mockMvc.perform(multipart("/api/vision/uploads").file(textFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("V002"));
    }
}
