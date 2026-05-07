package kr.spartaclub.aifriends.voice.service;

/**
 * TTS 합성 결과 + 메타데이터.
 *
 * <p>audio binary 만 들고 가면 어떤 프로바이더(OpenAI/ElevenLabs)가 어떤 voice 로 합성했는지
 * 호출자 쪽에서 알 수 없다. 응답 헤더({@code X-TTS-Provider} / {@code X-TTS-Voice}) 로 노출하기
 * 위해 메타데이터를 함께 돌려주는 record. 학생이 DevTools Network 탭만 열어도 swap 결과가
 * 보이도록 하는 게 의도다.</p>
 *
 * @param audio    합성된 오디오 byte[] (응답 본문)
 * @param provider 합성에 사용된 프로바이더 식별자 ({@code openai} / {@code elevenlabs} 등)
 * @param voice    합성에 사용된 실제 voice 식별자 (OpenAI 의 경우 {@code marin}, ElevenLabs 의
 *                 경우 UUID). 매핑 누락이면 null — 모델 기본값 사용을 의미.
 */
public record VoiceSynthesisResult(byte[] audio, String provider, String voice) {
}
