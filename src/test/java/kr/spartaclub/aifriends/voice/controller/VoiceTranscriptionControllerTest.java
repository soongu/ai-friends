package kr.spartaclub.aifriends.voice.controller;

import kr.spartaclub.aifriends.common.exception.GlobalExceptionHandler;
import kr.spartaclub.aifriends.voice.service.VoiceTranscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VoiceTranscriptionController.class)
@Import(GlobalExceptionHandler.class)
class VoiceTranscriptionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean VoiceTranscriptionService voiceTranscriptionService;

    @Test
    @DisplayName("정상 mp3 업로드는 200 + ApiResponse.success + data.text 가 STT 결과를 담는다")
    void should_return_200_with_text_when_valid_mp3_upload() throws Exception {
        when(voiceTranscriptionService.transcribe(any(Resource.class)))
                .thenReturn("안녕하세요");

        MockMultipartFile file = new MockMultipartFile(
                "audio",
                "hello.mp3",
                "audio/mpeg",
                "fake-mp3-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/voice/transcribe").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.text").value("안녕하세요"));
    }

    @Test
    @DisplayName("정상 wav 업로드도 200 + STT 결과를 돌려준다")
    void should_return_200_with_text_when_valid_wav_upload() throws Exception {
        when(voiceTranscriptionService.transcribe(any(Resource.class)))
                .thenReturn("hello world");

        MockMultipartFile file = new MockMultipartFile(
                "audio",
                "hello.wav",
                "audio/wav",
                "fake-wav-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/voice/transcribe").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.text").value("hello world"));
    }

    @Test
    @DisplayName("빈 파일은 400 + VC001 으로 응답한다")
    void should_return_400_when_audio_is_empty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "audio",
                "empty.mp3",
                "audio/mpeg",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/voice/transcribe").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VC001"));
    }

    @Test
    @DisplayName("지원하지 않는 확장자(.txt)는 400 + VC002 로 응답한다")
    void should_return_400_when_unsupported_extension() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "audio",
                "note.txt",
                "text/plain",
                "hello".getBytes()
        );

        mockMvc.perform(multipart("/api/voice/transcribe").file(textFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VC002"));
    }

    @Test
    @DisplayName("10MB 초과 파일은 400 + VC003 으로 응답한다")
    void should_return_400_when_audio_too_large() throws Exception {
        // 10MB + 1 byte
        byte[] tooBig = new byte[10 * 1024 * 1024 + 1];

        MockMultipartFile largeFile = new MockMultipartFile(
                "audio",
                "huge.mp3",
                "audio/mpeg",
                tooBig
        );

        mockMvc.perform(multipart("/api/voice/transcribe").file(largeFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VC003"));
    }
}
