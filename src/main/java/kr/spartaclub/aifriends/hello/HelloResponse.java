package kr.spartaclub.aifriends.hello;

/**
 * /api/hello-ai/v2 의 응답 페이로드.
 *
 * <ul>
 *   <li>provider  : 활성 프로파일의 모델/프로바이더 라벨 (예: "ollama-llama3.2:3b")</li>
 *   <li>message   : 사용자가 보낸 질문 그대로 (응답만으로 질문 추적 가능)</li>
 *   <li>reply     : LLM 본문</li>
 *   <li>latencyMs : ChatClient 호출 ~ 응답 수신까지 걸린 시간(밀리초)</li>
 * </ul>
 */
public record HelloResponse(
        String provider,
        String message,
        String reply,
        long latencyMs
) {
}
