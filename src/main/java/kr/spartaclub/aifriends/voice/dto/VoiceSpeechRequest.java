package kr.spartaclub.aifriends.voice.dto;

/**
 * Day 9 Step 6 — TTS 요청 DTO.
 *
 * <p>{@code POST /api/voice/speak} 의 JSON 본문을 매핑한다. 검증(null/blank, 최대 길이) 은
 * 컨트롤러에서 {@link kr.spartaclub.aifriends.voice.exception.VoiceException}
 * 으로 던져 표준 에러 응답으로 변환된다.</p>
 *
 * @param text  합성할 텍스트.
 * @param voice 사용할 보이스 식별자 (예: alloy/echo/fable/onyx/nova/shimmer). null/공백이면 모델 기본값.
 *              {@link org.springframework.ai.audio.tts.TextToSpeechOptions} 의 portable 옵션으로 전달된다.
 */
public record VoiceSpeechRequest(String text, String voice) {
    @Override
    public String toString() {
        return "VoiceSpeechRequest[text=*** (%d chars), voice=%s]".formatted(
                text == null ? 0 : text.length(), voice);
    }
}
