package kr.spartaclub.aifriends.voice.service;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.voice.exception.VoiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Day 9 Step 3 — Voice (STT, Speech-to-Text) 도메인 진입점.
 *
 * <p>음성 파일({@link Resource}) 한 개를 받아 {@link TranscriptionModel} 에 전달하고
 * 텍스트 결과를 그대로 돌려준다. {@link TranscriptionModel} 은 인터페이스로만 주입받으므로
 * (§4 프로바이더 추상화 게이트), 빈은 OpenAI `gpt-4o-transcribe` (디폴트) / `whisper-1` (레거시) · 추후 다른 STT 프로바이더 어떤 것이든
 * 될 수 있다 — 호출자는 모른다. 활성 STT 프로바이더는 {@code application.yml} 의
 * {@code spring.ai.model.audio.transcription} 프로퍼티로만 결정된다.</p>
 *
 * <p>응답 텍스트가 null/빈 문자열이면 빈 문자열을 반환한다 (Day 8 {@code VisionChatService}
 * 와 동일한 방어 패턴). null 입력 등 명백한 입력 오류는 {@link VoiceException} 으로
 * 래핑해 {@code GlobalExceptionHandler} 가 ApiResponse.fail 형태로 변환하게 한다.</p>
 *
 * <p>실제 STT 호출은 텍스트 LLM 대비 토큰 비용이 크다 (§9 비용 경고). 단위 테스트는
 * Mockito 모킹으로만 검증하고, 실제 호출 smoke 는 강사가 수동으로 한다.</p>
 */
@Slf4j
@Service
public class VoiceTranscriptionService {

    private final TranscriptionModel transcriptionModel;

    public VoiceTranscriptionService(TranscriptionModel transcriptionModel) {
        this.transcriptionModel = transcriptionModel;
    }

    /**
     * 음성 파일을 STT 모델에 보내 인식된 텍스트를 반환한다.
     *
     * @param audio 음성 파일 리소스 (MultipartFile 의 Resource 변환 또는 ClassPathResource)
     * @return 인식된 텍스트. 응답이 비어 있으면 빈 문자열.
     * @throws VoiceException audio 가 null 인 경우 (VOICE_AUDIO_REQUIRED)
     */
    public String transcribe(Resource audio) {
        if (audio == null) {
            throw new VoiceException(ErrorCode.VOICE_AUDIO_REQUIRED);
        }

        AudioTranscriptionResponse response = transcriptionModel.call(new AudioTranscriptionPrompt(audio));

        if (response == null || response.getResult() == null) {
            return "";
        }
        String text = response.getResult().getOutput();
        return text == null ? "" : text;
    }
}
