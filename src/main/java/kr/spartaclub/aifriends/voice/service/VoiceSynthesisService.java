package kr.spartaclub.aifriends.voice.service;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.voice.exception.VoiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.stereotype.Service;

/**
 * Day 9 Step 5 — Voice (TTS, Text-to-Speech) 도메인 진입점.
 *
 * <p>텍스트 한 문장을 받아 {@link TextToSpeechModel} 에 전달하고 합성된 오디오 바이트를
 * 그대로 돌려준다. {@link TextToSpeechModel} 은 인터페이스로만 주입받으므로
 * (§4 프로바이더 추상화 게이트), 빈은 OpenAI tts-1 · 추후 다른 TTS 프로바이더 어떤 것이든
 * 될 수 있다 — 호출자는 모른다. 활성 TTS 프로바이더는 {@code application.yml} 의
 * {@code spring.ai.model.audio.speech} 프로퍼티로만 결정된다.</p>
 *
 * <p>응답 byte[] 가 null 이면 빈 byte[] 를 반환한다 (Step 3 {@code VoiceTranscriptionService}
 * 와 동일한 방어 패턴). null/빈 텍스트 등 명백한 입력 오류는 {@link VoiceException} 으로
 * 래핑해 {@code GlobalExceptionHandler} 가 ApiResponse.fail 형태로 변환하게 한다.</p>
 *
 * <p>실제 TTS 호출은 텍스트 LLM 대비 토큰 비용이 크다 (§9 비용 경고). 단위 테스트는
 * Mockito 모킹으로만 검증하고, 실제 호출 smoke 는 강사가 수동으로 한다.</p>
 */
@Slf4j
@Service
public class VoiceSynthesisService {

    private final TextToSpeechModel textToSpeechModel;

    public VoiceSynthesisService(TextToSpeechModel textToSpeechModel) {
        this.textToSpeechModel = textToSpeechModel;
    }

    /**
     * 텍스트를 TTS 모델에 보내 합성된 오디오 바이트를 반환한다.
     *
     * @param text 합성할 텍스트 (null/빈 문자열 금지)
     * @return 합성된 오디오 byte[] (응답이 비어 있으면 빈 byte[])
     * @throws VoiceException text 가 null 또는 공백뿐인 경우 (VOICE_TEXT_REQUIRED)
     */
    public byte[] synthesize(String text) {
        if (text == null || text.isBlank()) {
            throw new VoiceException(ErrorCode.VOICE_TEXT_REQUIRED);
        }

        TextToSpeechResponse response = textToSpeechModel.call(new TextToSpeechPrompt(text));

        if (response == null || response.getResult() == null) {
            return new byte[0];
        }
        Speech speech = response.getResult();
        byte[] audio = speech.getOutput();
        return audio == null ? new byte[0] : audio;
    }
}
