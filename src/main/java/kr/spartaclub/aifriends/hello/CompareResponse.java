package kr.spartaclub.aifriends.hello;

import java.util.List;

/**
 * /api/compare 의 응답 페이로드.
 *
 * <p>여러 프로바이더에 같은 질문을 병렬로 던진 결과를 한 배열에 담아
 * "같은 질문에 대한 서로 다른 모델의 반응" 을 한눈에 비교할 수 있게 한다.</p>
 *
 * <ul>
 *   <li>message : 요청에 썼던 질문 그대로</li>
 *   <li>results : 각 프로바이더별 응답 레코드. 한 쪽이 실패해도 에러 메시지를 {@code reply} 에 담아 반환.</li>
 * </ul>
 */
public record CompareResponse(
        String message,
        List<ProviderResult> results
) {
    public record ProviderResult(
            String provider,
            String reply,
            long latencyMs
    ) {
    }
}
