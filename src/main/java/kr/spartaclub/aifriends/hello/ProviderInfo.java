package kr.spartaclub.aifriends.hello;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 현재 활성 ChatModel 의 "사람이 읽을 수 있는 이름" 을 만들어 준다.
 *
 * <p>동작:
 * <ul>
 *   <li>spring.ai.model.chat=ollama → "ollama-" + spring.ai.ollama.chat.options.model</li>
 *   <li>spring.ai.model.chat=openai → base-url 도메인에 따라 gemini/groq/openai 중 하나로 라벨링</li>
 *   <li>그 외(none 포함) → "none"</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ProviderInfo {

    private final Environment env;

    /** 응답에 박을 짧은 라벨. 예: "ollama-llama3.2:3b", "gemini-2.5-flash-lite". */
    public String currentLabel() {
        String chatType = env.getProperty("spring.ai.model.chat", "none");

        return switch (chatType) {
            case "ollama" -> "ollama-" + env.getProperty(
                    "spring.ai.ollama.chat.options.model", "unknown");
            case "openai" -> openAiLabel();
            default -> "none";
        };
    }

    /**
     * spring.ai.openai 는 "OpenAI 호환" 이라는 점만 약속한다.
     * 실제로 어디로 호출하는지는 base-url 에 따라 달라지므로 라벨도 거기에 맞춰 구분한다.
     * 모델명이 이미 프로바이더 접두사로 시작하면(예: "gemini-2.5-flash-lite") 중복을 피한다.
     */
    private String openAiLabel() {
        String baseUrl = env.getProperty("spring.ai.openai.base-url", "");
        String model = env.getProperty(
                "spring.ai.openai.chat.options.model", "unknown");

        if (baseUrl.contains("googleapis.com")) return prefix("gemini-", model);
        if (baseUrl.contains("groq.com")) return prefix("groq-", model);
        return prefix("openai-", model);
    }

    private String prefix(String tag, String model) {
        return model.startsWith(tag) ? model : tag + model;
    }
}
