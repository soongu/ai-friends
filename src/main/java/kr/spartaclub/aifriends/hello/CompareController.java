package kr.spartaclub.aifriends.hello;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Day 2 과제 2 — 두 프로바이더에 같은 질문을 병렬로 던져 응답을 한 번에 비교하는 엔드포인트.
 *
 * <p>{@code @Qualifier} 로 "ollamaChatModel" / "geminiChatModel" 두 개의 {@link ChatModel}
 * 빈을 주입받고, 각각 {@link CompletableFuture#supplyAsync} 로 독립 쓰레드에서 호출한다.
 * 둘의 latency 합이 순차 호출 대비 "둘 중 더 느린 쪽 + α" 로 줄어드는 게 병렬의 증거.</p>
 *
 * <p>한 프로바이더가 예외를 던져도 다른 쪽 응답은 그대로 반환되도록
 * {@link #callSafely} 에서 per-provider 로 예외를 포획하고 에러 메시지를 {@code reply} 에 담는다.
 * (플레이그라운드 UX 에서 한 쪽이 장애여도 다른 쪽은 보여야 하는 패턴)</p>
 */
@RestController
@Profile("compare")
public class CompareController {

    private final ChatModel ollamaChatModel;
    private final ChatModel geminiChatModel;
    private final String ollamaLabel;
    private final String geminiLabel;

    public CompareController(
            @Qualifier("ollamaChatModel") ChatModel ollamaChatModel,
            @Qualifier("geminiChatModel") ChatModel geminiChatModel,
            @Value("${app.compare.ollama.model}") String ollamaModel,
            @Value("${app.compare.gemini.model}") String geminiModel
    ) {
        this.ollamaChatModel = ollamaChatModel;
        this.geminiChatModel = geminiChatModel;
        this.ollamaLabel = prefix("ollama-", ollamaModel);
        this.geminiLabel = prefix("gemini-", geminiModel);
    }

    /** 모델명이 이미 프로바이더 접두사로 시작하면(예: "gemini-2.5-flash-lite") 중복을 피한다. */
    private static String prefix(String tag, String model) {
        return model.startsWith(tag) ? model : tag + model;
    }

    @GetMapping("/api/compare")
    public CompareResponse compare(
            @RequestParam(defaultValue = "Hello, AI! 한 줄로 자기소개 부탁해.") String message
    ) {
        CompletableFuture<CompareResponse.ProviderResult> ollamaFuture =
                CompletableFuture.supplyAsync(() -> callSafely(ollamaChatModel, message, ollamaLabel));
        CompletableFuture<CompareResponse.ProviderResult> geminiFuture =
                CompletableFuture.supplyAsync(() -> callSafely(geminiChatModel, message, geminiLabel));

        return new CompareResponse(
                message,
                List.of(ollamaFuture.join(), geminiFuture.join())
        );
    }

    private CompareResponse.ProviderResult callSafely(ChatModel model, String message, String label) {
        long start = System.currentTimeMillis();
        try {
            ChatResponse response = model.call(new Prompt(message));
            String reply = response.getResult().getOutput().getText();
            return new CompareResponse.ProviderResult(
                    label, reply, System.currentTimeMillis() - start);
        } catch (Exception ex) {
            return new CompareResponse.ProviderResult(
                    label,
                    "ERROR: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
                    System.currentTimeMillis() - start
            );
        }
    }
}
