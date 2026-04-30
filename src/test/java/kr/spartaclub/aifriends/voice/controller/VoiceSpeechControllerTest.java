package kr.spartaclub.aifriends.voice.controller;

import kr.spartaclub.aifriends.common.exception.GlobalExceptionHandler;
import kr.spartaclub.aifriends.voice.service.VoiceSynthesisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VoiceSpeechController.class)
@Import(GlobalExceptionHandler.class)
class VoiceSpeechControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean VoiceSynthesisService voiceSynthesisService;

    @Test
    @DisplayName("정상 텍스트는 200 + Content-Type audio/mpeg + 합성된 byte[] 를 그대로 돌려준다")
    void should_return_200_with_audio_mpeg_bytes_when_valid_text() throws Exception {
        byte[] stubAudio = new byte[]{0x49, 0x44, 0x33, 0x04, 0x00};  // ID3 헤더 흉내
        when(voiceSynthesisService.synthesize(anyString())).thenReturn(stubAudio);

        mockMvc.perform(post("/api/voice/speak")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"안녕하세요\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "audio/mpeg"))
                .andExpect(content().bytes(stubAudio));
    }

    @Test
    @DisplayName("text 가 빈 문자열이면 400 + VC005 ApiResponse 에러 응답")
    void should_return_400_when_text_is_blank() throws Exception {
        mockMvc.perform(post("/api/voice/speak")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VC005"));
    }

    @Test
    @DisplayName("text 가 null 이면 400 + VC005 ApiResponse 에러 응답")
    void should_return_400_when_text_is_null() throws Exception {
        mockMvc.perform(post("/api/voice/speak")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VC005"));
    }

    @Test
    @DisplayName("text 가 4000자를 초과하면 400 + VC007 ApiResponse 에러 응답")
    void should_return_400_when_text_too_long() throws Exception {
        String tooLong = "가".repeat(4001);

        mockMvc.perform(post("/api/voice/speak")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VC007"));
    }
}
