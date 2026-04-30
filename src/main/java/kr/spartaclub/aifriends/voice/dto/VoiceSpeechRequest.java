package kr.spartaclub.aifriends.voice.dto;

/**
 * Day 9 Step 6 — TTS 요청 DTO.
 *
 * <p>{@code POST /api/voice/speak} 의 JSON 본문을 매핑한다.
 * {@code text} 필드 한 개만 받는 단순한 record. 검증(null/blank, 최대 길이) 은
 * 컨트롤러에서 {@link kr.spartaclub.aifriends.voice.exception.VoiceException}
 * 으로 던져 표준 에러 응답으로 변환된다.</p>
 */
public record VoiceSpeechRequest(String text) {
}
