package kr.spartaclub.aifriends.voice.service;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.voice.exception.VoiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Day 9 Step 3 — VoiceTranscriptionService 단위 테스트.
 *
 * <p>외부 STT 프로바이더(OpenAI Whisper 등) 호출은 전부 모킹한다 (실제 호출 비용 큼 — §9 비용 경고).
 * 핵심은 음성 {@link Resource} 가 {@link AudioTranscriptionPrompt} 로 감싸져
 * {@link TranscriptionModel#call(AudioTranscriptionPrompt)} 에 전달되고,
 * 응답의 텍스트가 그대로 반환되는지를 ArgumentCaptor 로 검증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class VoiceTranscriptionServiceTest {

    @Mock
    TranscriptionModel transcriptionModel;

    private VoiceTranscriptionService sut() {
        return new VoiceTranscriptionService(transcriptionModel);
    }

    @Test
    @DisplayName("Resource → TranscriptionModel.call 에 AudioTranscriptionPrompt 가 그대로 전달된다")
    void should_send_audio_resource_when_transcribe() {
        // given
        Resource audio = new ByteArrayResource("fake-audio-bytes".getBytes()) {
            @Override
            public String getFilename() {
                return "sample.wav";
            }
        };
        when(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
                .thenReturn(fakeResponse("안녕하세요, 반갑습니다."));

        // when
        sut().transcribe(audio);

        // then
        ArgumentCaptor<AudioTranscriptionPrompt> captor = ArgumentCaptor.forClass(AudioTranscriptionPrompt.class);
        verify(transcriptionModel).call(captor.capture());

        AudioTranscriptionPrompt sentPrompt = captor.getValue();
        assertThat(sentPrompt.getInstructions()).isSameAs(audio);
    }

    @Test
    @DisplayName("응답의 transcription 텍스트가 그대로 반환된다")
    void should_return_transcription_text_when_response_present() {
        Resource audio = new ByteArrayResource("fake".getBytes());
        when(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
                .thenReturn(fakeResponse("오늘 날씨 어때?"));

        String result = sut().transcribe(audio);

        assertThat(result).isEqualTo("오늘 날씨 어때?");
    }

    @Test
    @DisplayName("응답 텍스트가 null 이면 빈 문자열을 반환한다 (예외 던지지 않음)")
    void should_return_empty_string_when_response_text_null() {
        Resource audio = new ByteArrayResource("fake".getBytes());
        when(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
                .thenReturn(fakeResponse(null));

        String result = sut().transcribe(audio);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("audio 가 null 이면 VoiceException(VOICE_AUDIO_REQUIRED) 을 던진다")
    void should_throw_voice_exception_when_audio_null() {
        assertThatThrownBy(() -> sut().transcribe(null))
                .isInstanceOf(VoiceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VOICE_AUDIO_REQUIRED);
    }

    private AudioTranscriptionResponse fakeResponse(String text) {
        return new AudioTranscriptionResponse(new AudioTranscription(text));
    }
}
