package kr.spartaclub.aifriends.hello;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;

/**
 * Day 2 과제 1 — 프로바이더 성능 벤치마크 엔드포인트.
 *
 * <p>같은 질문을 iterations 횟수만큼 순차 호출하여 latency 통계를 반환한다.
 * 단일 호출은 네트워크 편차가 커서 모델 비교에 부적합하기 때문에
 * 통계적 감각을 잡기 위함.</p>
 *
 * <p>{@link ChatClient} 만 주입하므로 활성 프로파일이 ollama/gemini/groq 어느 쪽이든
 * 컨트롤러 코드는 변하지 않는다 — Day 2 Step 1 에서 다룬 프로바이더 추상화의 실증.</p>
 */
@RestController
public class BenchmarkController {

    private static final int MIN_ITERATIONS = 1;
    private static final int MAX_ITERATIONS = 10;

    private final ChatClient chatClient;
    private final ProviderInfo providerInfo;

    public BenchmarkController(ChatClient.Builder builder, ProviderInfo providerInfo) {
        this.chatClient = builder.build();
        this.providerInfo = providerInfo;
    }

    @GetMapping("/api/benchmark")
    public BenchmarkResponse benchmark(
            @RequestParam(defaultValue = "Hello, AI! 한 줄로 자기소개 부탁해.") String message,
            @RequestParam(defaultValue = "3") int iterations
    ) {
        if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
            throw new IllegalArgumentException(
                    "iterations must be between " + MIN_ITERATIONS + " and " + MAX_ITERATIONS
            );
        }

        List<Long> allMs = new ArrayList<>(iterations);
        String lastReply = null;

        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();
            lastReply = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();
            allMs.add(System.currentTimeMillis() - start);
        }

        LongSummaryStatistics stats = allMs.stream()
                .mapToLong(Long::longValue)
                .summaryStatistics();

        return new BenchmarkResponse(
                providerInfo.currentLabel(),
                message,
                iterations,
                new BenchmarkResponse.LatencyStats(
                        stats.getMin(),
                        stats.getMax(),
                        stats.getAverage(),
                        allMs
                ),
                lastReply
        );
    }

    /**
     * iterations 범위 위반 시 400 응답을 명시적으로 만들어 준다.
     * (전역 @ControllerAdvice 가 없는 슬라이스 테스트에서도 안정적으로 400 으로 떨어지게 하기 위함)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
