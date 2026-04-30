package kr.spartaclub.aifriends.vision.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Day 8 Step 4 — VisionChatService 단위 테스트.
 *
 * <p>외부 프로바이더 호출은 전부 모킹 (Gemini · OpenAI · Ollama 어떤 것도 실제 호출 금지).
 * Vision API 의 핵심은 {@link UserMessage} 가 텍스트 + {@link Media} 1장을 함께 들고
 * {@link ChatModel#call(Prompt)} 로 전달되는 것 — 그 한 가지를 ArgumentCaptor 로 검증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class VisionChatServiceTest {

    @Mock
    ChatModel chatModel;

    private VisionChatService sut() {
        return new VisionChatService(chatModel);
    }

    @Test
    @DisplayName("URL + 프롬프트 → ChatModel 에 UserMessage(text + media 1장) 가 담긴 Prompt 가 전달된다")
    void should_send_user_message_with_one_media_when_describe() {
        // given
        String imageUrl = "https://image.pollinations.ai/prompt/portrait?model=flux";
        String prompt = "이 인물의 표정을 한 문장으로 묘사해줘";
        when(chatModel.call(any(Prompt.class))).thenReturn(fakeResponse("차분한 미소를 머금고 있습니다."));

        // when
        sut().describe(imageUrl, prompt);

        // then
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());

        List<Message> messages = captor.getValue().getInstructions();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).isInstanceOf(UserMessage.class);

        UserMessage userMessage = (UserMessage) messages.get(0);
        assertThat(userMessage.getText()).isEqualTo(prompt);
        assertThat(userMessage.getMedia()).hasSize(1);
        assertThat(userMessage.getMedia().get(0).getMimeType()).isNotNull();
    }

    @Test
    @DisplayName("ChatModel 응답 텍스트가 그대로 반환된다")
    void should_return_assistant_message_text_when_response_present() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(fakeResponse("차분한 미소를 머금고 있습니다."));

        String result = sut().describe("https://example.com/a.png", "묘사해줘");

        assertThat(result).isEqualTo("차분한 미소를 머금고 있습니다.");
    }

    @Test
    @DisplayName("응답 본문이 비어 있으면 빈 문자열을 반환한다 (예외 던지지 않음)")
    void should_return_empty_string_when_response_text_blank() {
        when(chatModel.call(any(Prompt.class))).thenReturn(fakeResponse(""));

        String result = sut().describe("https://example.com/a.png", "묘사해줘");

        assertThat(result).isEmpty();
    }

    private ChatResponse fakeResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
