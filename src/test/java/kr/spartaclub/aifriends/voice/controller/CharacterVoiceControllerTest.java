package kr.spartaclub.aifriends.voice.controller;

import kr.spartaclub.aifriends.common.exception.GlobalExceptionHandler;
import kr.spartaclub.aifriends.voice.service.CharacterVoiceService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CharacterVoiceController.class)
@Import(GlobalExceptionHandler.class)
class CharacterVoiceControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean CharacterVoiceService characterVoiceService;

    @Test
    @DisplayName("정상 mp3 업로드는 200 + Content-Type audio/mpeg + 합성된 byte[] 를 그대로 돌려준다")
    void should_return_200_with_audio_mpeg_bytes_when_valid_upload() throws Exception {
        byte[] stubAudio = new byte[]{0x49, 0x44, 0x33, 0x04};
        when(characterVoiceService.converse(eq(1L), any(Resource.class))).thenReturn(stubAudio);

        MockMultipartFile file = new MockMultipartFile(
                "audio",
                "hello.mp3",
                "audio/mpeg",
                "fake-mp3-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/voice/characters/1/converse").file(file))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "audio/mpeg"))
                .andExpect(content().bytes(stubAudio));
    }

    @Test
    @DisplayName("빈 파일은 400 + VC001 ApiResponse 에러 응답")
    void should_return_400_when_audio_is_empty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "audio", "empty.mp3", "audio/mpeg", new byte[0]
        );

        mockMvc.perform(multipart("/api/voice/characters/1/converse").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VC001"));
    }

    @Test
    @DisplayName("지원하지 않는 확장자(.txt) 는 400 + VC002 ApiResponse 에러 응답")
    void should_return_400_when_unsupported_extension() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "audio", "note.txt", "text/plain", "hello".getBytes()
        );

        mockMvc.perform(multipart("/api/voice/characters/1/converse").file(textFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VC002"));
    }
}
