package kr.spartaclub.aifriends.hello;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 활성 프로파일에 따라 "사람이 읽을 수 있는" 프로바이더 라벨을 만들어주는
 * 컴포넌트의 단위 테스트. Spring 컨텍스트를 띄우지 않고 MockEnvironment 만 사용한다.
 */
class ProviderInfoTest {

    @Test
    @DisplayName("spring.ai.model.chat=ollama → 'ollama-<model>' 라벨")
    void ollamaLabel() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.ai.model.chat", "ollama")
                .withProperty("spring.ai.ollama.chat.options.model", "llama3.2:3b");

        assertThat(new ProviderInfo(env).currentLabel())
                .isEqualTo("ollama-llama3.2:3b");
    }

    @Test
    @DisplayName("openai + base-url=googleapis → 'gemini-<model>' 라벨")
    void geminiLabel() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.ai.model.chat", "openai")
                .withProperty("spring.ai.openai.base-url",
                        "https://generativelanguage.googleapis.com/v1beta/openai")
                .withProperty("spring.ai.openai.chat.options.model", "gemini-2.5-flash-lite");

        assertThat(new ProviderInfo(env).currentLabel())
                .isEqualTo("gemini-2.5-flash-lite");
    }

    @Test
    @DisplayName("openai + base-url=groq.com → 'groq-<model>' 라벨")
    void groqLabel() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.ai.model.chat", "openai")
                .withProperty("spring.ai.openai.base-url", "https://api.groq.com/openai/v1")
                .withProperty("spring.ai.openai.chat.options.model", "llama-3.3-70b-versatile");

        assertThat(new ProviderInfo(env).currentLabel())
                .isEqualTo("groq-llama-3.3-70b-versatile");
    }

    @Test
    @DisplayName("openai + base-url=openai.com → 'openai-<model>' 라벨")
    void openaiLabel() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.ai.model.chat", "openai")
                .withProperty("spring.ai.openai.base-url", "https://api.openai.com/v1")
                .withProperty("spring.ai.openai.chat.options.model", "gpt-4o-mini");

        assertThat(new ProviderInfo(env).currentLabel())
                .isEqualTo("openai-gpt-4o-mini");
    }

    @Test
    @DisplayName("spring.ai.model.chat 프로퍼티가 없으면 'none' 을 반환한다")
    void noneLabel() {
        assertThat(new ProviderInfo(new MockEnvironment()).currentLabel())
                .isEqualTo("none");
    }
}
