package kr.spartaclub.aifriends.voice.service;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.voice.exception.VoiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechOptions;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.elevenlabs.ElevenLabsTextToSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

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
 * Mockito 모킹으로만 검증하고, 실제 호출 smoke 는 사용자가 수동으로 한다.</p>
 */
@Slf4j
@Service
public class VoiceSynthesisService {

    /**
     * mood key → OpenAI 보이스 식별자 (gpt-4o-mini-tts 의 13종 중 4종 사용).
     *
     * <p>2026 년 OpenAI 가 추가한 marin · cedar 는 공식적으로 "best quality" 로 권장되고
     * gpt-4o-mini-tts 전용이다 (tts-1/tts-1-hd 같은 레거시 모델은 미지원 — 모델 다운그레이드
     * 시 이 매핑을 alloy/nova 류로 폴백해야 한다).</p>
     */
    private static final Map<String, String> OPENAI_VOICE_BY_MOOD = Map.of(
            "bright",   "marin",
            "warm",     "coral",
            "calm",     "cedar",
            "cheerful", "ash"
    );

    /**
     * mood key → ElevenLabs 보이스 UUID (eleven_multilingual_v2 모델로 한국어 가능).
     *
     * <p>ElevenLabs 는 voice 식별자가 UUID 다 (이름 아님). 아래 4 개는 ElevenLabs prebuilt voice
     * 라이브러리에 안정적으로 존재하는 무료 티어 화자들이다. 학생이 ElevenLabs 계정의 "Voices"
     * 탭에서 선호하는 voice 를 골라 UUID 를 복사해 이 표에 갈아 끼울 수 있다.</p>
     */
    private static final Map<String, String> ELEVENLABS_VOICE_BY_MOOD = Map.of(
            "bright",   "5I7B1di44aCL15NkP0jn", // Kanna — 자연 narrator 여성
            "warm",     "5n5gqmaQi9Ewevrz7bOS", // Sian  — calm/soft 여성
            "calm",     "pNInz6obpgDQGcFmaJgB", // Adam   — deep narrator 남성
            "cheerful", "ErXwobaYiN019PkySvjV"  // Antoni — 활기 narrator 남성
    );

    private final TextToSpeechModel textToSpeechModel;
    private final String activeProvider;

    public VoiceSynthesisService(
            TextToSpeechModel textToSpeechModel,
            @Value("${spring.ai.model.audio.speech:openai}") String activeProvider) {
        this.textToSpeechModel = textToSpeechModel;
        this.activeProvider = activeProvider;
    }

    /**
     * 텍스트를 TTS 모델에 보내 합성된 오디오 바이트를 반환한다 (보이스 미지정 — 모델 기본값).
     *
     * @param text 합성할 텍스트 (null/빈 문자열 금지)
     * @return 합성된 오디오 byte[] (응답이 비어 있으면 빈 byte[])
     * @throws VoiceException text 가 null 또는 공백뿐인 경우 (VOICE_TEXT_REQUIRED)
     */
    public byte[] synthesize(String text) {
        return synthesize(text, null);
    }

    /**
     * 텍스트를 TTS 모델에 보내 합성된 오디오 바이트를 반환한다.
     *
     * <p>{@code voice} 인자는 두 가지 형태를 모두 받는다:
     * <ul>
     *   <li><b>추상 mood key</b> ({@code bright/warm/calm/cheerful}) — 활성 프로바이더의 적절한
     *       voice 식별자로 자동 매핑된다. 프론트는 이 키만 보내면 .env 의 {@code TTS_PROVIDER}
     *       값에 따라 OpenAI marin / ElevenLabs Rachel UUID 등으로 자연스럽게 swap 된다.</li>
     *   <li><b>raw voice id</b> — 매핑 테이블에 없는 값이면 프로바이더 식별자로 보고 그대로 전달.</li>
     * </ul>
     * Spring AI {@link TextToSpeechOptions} 의 portable {@code voice} 슬롯에 박히므로 OpenAI
     * 와 ElevenLabs 모두 동일 코드로 처리된다.
     *
     * @param text  합성할 텍스트 (null/빈 문자열 금지)
     * @param voice 추상 mood key 또는 raw voice id (null/공백이면 모델 기본값)
     * @return 합성된 오디오 byte[] (응답이 비어 있으면 빈 byte[])
     * @throws VoiceException text 가 null 또는 공백뿐인 경우 (VOICE_TEXT_REQUIRED)
     */
    public byte[] synthesize(String text, String voice) {
        return synthesizeWithMeta(text, voice).audio();
    }

    /**
     * 합성 결과 + 메타데이터(활성 프로바이더 / 실제 사용된 voice 식별자) 를 한 덩어리로 돌려준다.
     * 컨트롤러가 응답 헤더 {@code X-TTS-Provider} / {@code X-TTS-Voice} 로 박을 때 사용한다.
     */
    public VoiceSynthesisResult synthesizeWithMeta(String text, String voice) {
        if (text == null || text.isBlank()) {
            throw new VoiceException(ErrorCode.VOICE_TEXT_REQUIRED);
        }

        String resolvedVoice = resolveVoice(voice);

        // ⚠️ Spring AI 1.1 의 ElevenLabsTextToSpeechModel 은 portable TextToSpeechOptions 의
        //    voice() 슬롯을 무시한다 (instanceof check 로 ElevenLabsTextToSpeechOptions 만 인식).
        //    OpenAI 도 동일 — 안전하게 매번 provider-specific options 객체로 빌드한다.
        //    덕분에 캐릭터별 voice 매핑이 양쪽 프로바이더에서 동일하게 동작한다.
        TextToSpeechOptions providerOptions = buildProviderOptions(resolvedVoice);
        TextToSpeechPrompt prompt = providerOptions != null
                ? new TextToSpeechPrompt(text, providerOptions)
                : new TextToSpeechPrompt(text);

        TextToSpeechResponse response = textToSpeechModel.call(prompt);

        byte[] audio;
        if (response == null || response.getResult() == null) {
            audio = new byte[0];
        } else {
            Speech speech = response.getResult();
            byte[] out = speech.getOutput();
            audio = out == null ? new byte[0] : out;
        }

        log.info("[VoiceSynthesis] provider={}, requestedVoice='{}', resolvedVoice='{}', textLength={}, audioBytes={}",
                activeProvider, voice, resolvedVoice, text.length(), audio.length);

        return new VoiceSynthesisResult(audio, activeProvider, resolvedVoice);
    }

    /** 활성 TTS 프로바이더 식별자 — {@code /api/voice/info} 엔드포인트 등에서 노출한다. */
    public String getActiveProvider() {
        return activeProvider;
    }

    /**
     * 활성 프로바이더에 맞는 provider-specific options 객체를 빌드한다.
     *
     * <p>왜 portable {@link TextToSpeechOptions} 를 못 쓰나: Spring AI 1.1 의
     * {@code ElevenLabsTextToSpeechModel} 은 {@code prompt.getOptions() instanceof
     * ElevenLabsTextToSpeechOptions} 체크로만 runtime options 를 인식한다 (문서:
     * docs.spring.io/spring-ai/reference/api/audio/speech/elevenlabs-speech.html). portable
     * 인터페이스로 voice 를 박으면 type 이 안 맞아서 무시되고, application.yml 의 default
     * voice-id (Rachel UUID) 로 폴백한다 — 그래서 캐릭터별 swap 이 동작하지 않았다.
     * OpenAI starter 도 동일한 패턴이라 양쪽 모두 명시 빌드가 안전하다.</p>
     */
    private TextToSpeechOptions buildProviderOptions(String resolvedVoice) {
        if (!StringUtils.hasText(resolvedVoice)) return null;

        if ("elevenlabs".equalsIgnoreCase(activeProvider)) {
            // ElevenLabs 는 voiceId 슬롯에 UUID 박는다. modelId/outputFormat 은 application.yml 의
            // default 가 자동 적용되므로 여기서는 voiceId 만 override.
            return ElevenLabsTextToSpeechOptions.builder()
                    .voiceId(resolvedVoice)
                    .build();
        }
        // 기본: OpenAI 의 voice 슬롯에 식별자(marin/cedar 등) 박는다.
        return OpenAiAudioSpeechOptions.builder()
                .voice(resolvedVoice)
                .build();
    }

    /**
     * 추상 mood key 면 활성 프로바이더의 voice 식별자로 매핑한다.
     * 매핑 테이블에 없는 값은 raw voice id 로 간주해 그대로 돌려준다 (학습용 escape hatch).
     */
    private String resolveVoice(String voiceOrMood) {
        if (!StringUtils.hasText(voiceOrMood)) return null;

        Map<String, String> table = "elevenlabs".equalsIgnoreCase(activeProvider)
                ? ELEVENLABS_VOICE_BY_MOOD
                : OPENAI_VOICE_BY_MOOD;

        String mapped = table.get(voiceOrMood);
        if (mapped != null) {
            log.debug("[VoiceSynthesis] mood='{}' → provider='{}' voice='{}'", voiceOrMood, activeProvider, mapped);
            return mapped;
        }
        // 매핑 테이블에 없으면 raw voice id 로 본다 (디버그/학습용 escape).
        return voiceOrMood;
    }
}
