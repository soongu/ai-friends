package kr.spartaclub.aifriends.voice.service;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.voice.exception.VoiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Day 9 Step 5 — VoiceSynthesisService 단위 테스트.
 *
 * <p>외부 TTS 프로바이더(OpenAI tts-1 등) 호출은 전부 모킹한다 (실제 호출 비용 큼 — §9 비용 경고).
 * 핵심은 텍스트가 {@link TextToSpeechPrompt} 로 감싸져
 * {@link TextToSpeechModel#call(TextToSpeechPrompt)} 에 전달되고,
 * 응답의 {@link Speech#getOutput()} 바이트가 그대로 반환되는지를 ArgumentCaptor 로 검증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class VoiceSynthesisServiceTest {

    @Mock
    TextToSpeechModel textToSpeechModel;

    private VoiceSynthesisService sut() {
        return new VoiceSynthesisService(textToSpeechModel);
    }

    @Test
    @DisplayName("텍스트 → TextToSpeechModel.call 에 TextToSpeechPrompt(text) 가 그대로 전달된다")
    void should_send_text_when_synthesize() {
        // given
        byte[] fakeAudio = new byte[]{1, 2, 3, 4};
        when(textToSpeechModel.call(any(TextToSpeechPrompt.class)))
                .thenReturn(fakeResponse(fakeAudio));

        // when
        byte[] result = sut().synthesize("안녕하세요, 반갑습니다.");

        // then
        ArgumentCaptor<TextToSpeechPrompt> captor = ArgumentCaptor.forClass(TextToSpeechPrompt.class);
        org.mockito.Mockito.verify(textToSpeechModel).call(captor.capture());

        TextToSpeechPrompt sentPrompt = captor.getValue();
        assertThat(sentPrompt.getInstructions().getText()).isEqualTo("안녕하세요, 반갑습니다.");
        assertThat(result).isEqualTo(fakeAudio);
    }

    @Test
    @DisplayName("응답 byte[] 가 null 이면 빈 byte[] 를 반환한다 (예외 던지지 않음)")
    void should_return_empty_bytes_when_response_output_null() {
        when(textToSpeechModel.call(any(TextToSpeechPrompt.class)))
                .thenReturn(fakeResponse(null));

        byte[] result = sut().synthesize("hello");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("text 가 null 이면 VoiceException(VOICE_TEXT_REQUIRED) 을 던진다")
    void should_throw_voice_exception_when_text_null() {
        assertThatThrownBy(() -> sut().synthesize(null))
                .isInstanceOf(VoiceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VOICE_TEXT_REQUIRED);
    }

    @Test
    @DisplayName("text 가 빈 문자열이면 VoiceException(VOICE_TEXT_REQUIRED) 을 던진다")
    void should_throw_voice_exception_when_text_blank() {
        assertThatThrownBy(() -> sut().synthesize("   "))
                .isInstanceOf(VoiceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VOICE_TEXT_REQUIRED);
    }

    private TextToSpeechResponse fakeResponse(byte[] audio) {
        return new TextToSpeechResponse(List.of(new Speech(audio)));
    }
}
