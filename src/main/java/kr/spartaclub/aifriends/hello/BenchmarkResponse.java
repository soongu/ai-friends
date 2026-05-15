package kr.spartaclub.aifriends.hello;

import java.util.List;

/**
 * /api/benchmark 의 응답 페이로드.
 *
 * <ul>
 *   <li>provider     : 활성 프로파일의 모델/프로바이더 라벨 (예: "gemini-2.5-flash-lite")</li>
 *   <li>message      : 벤치마크에 쓴 질문 그대로</li>
 *   <li>iterations   : 실제 호출 횟수</li>
 *   <li>latencyStats : 호출별 latency 의 min/max/avg 와 전체 배열</li>
 *   <li>sampleReply  : 마지막 호출의 응답 (참고용)</li>
 * </ul>
 */
public record BenchmarkResponse(
        String provider,
        String message,
        int iterations,
        LatencyStats latencyStats,
        String sampleReply
) {
    public record LatencyStats(
            long minMs,
            long maxMs,
            double avgMs,
            List<Long> allMs
    ) {
    }
}
