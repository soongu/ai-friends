package kr.spartaclub.aifriends.voice.dto;

/**
 * Day 9 Step 4 — STT 응답 DTO.
 *
 * <p>{@code POST /api/voice/transcribe} 의 정상 응답 본문에 들어가는 record. 학생이 처음 마주치는
 * "음성 → 텍스트" 변환 결과이므로 일부러 필드를 한 개({@code text})만 둔다. 추후 confidence/segments
 * 같은 부가 정보가 필요해지면 필드를 추가해 확장한다.</p>
 *
 * @param text STT 모델이 인식한 텍스트. 비어 있는 음성이거나 모델이 아무것도 인식하지 못하면 빈 문자열.
 */
public record VoiceTranscriptionResponse(
        String text
) {
    @Override
    public String toString() {
        return "VoiceTranscriptionResponse[text=*** (%d chars)]".formatted(
                text == null ? 0 : text.length());
    }
}
