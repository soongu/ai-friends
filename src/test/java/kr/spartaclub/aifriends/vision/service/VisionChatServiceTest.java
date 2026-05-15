package kr.spartaclub.aifriends.vision.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
 *
 * <p>Day 8 retrofit (2026-05-14): 로컬 업로드 경로(`/uploads/...`) 는 디스크에서 읽어
 * base64 인라인으로 보내고, 외부 HTTPS URL 은 URI 패스스루로 보내는 두 갈래를 검증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class VisionChatServiceTest {

    @Mock
    ChatModel chatModel;

    private VisionChatService sut() {
        return new VisionChatService(chatModel, "./uploads");
    }

    private VisionChatService sut(String uploadBaseDir) {
        return new VisionChatService(chatModel, uploadBaseDir);
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

    @Test
    @DisplayName("외부 HTTPS URL 은 URI 문자열 그대로 Media 에 전달된다 (패스스루)")
    void should_pass_external_url_as_uri_string_when_describe() {
        // given
        String imageUrl = "https://image.pollinations.ai/prompt/portrait?model=flux";
        when(chatModel.call(any(Prompt.class))).thenReturn(fakeResponse("ok"));

        // when
        sut().describe(imageUrl, "묘사해줘");

        // then
        UserMessage userMessage = captureUserMessage();
        Object data = userMessage.getMedia().get(0).getData();
        // Media.builder().data(URI) 는 내부에 URI.toString() (String) 으로 저장한다.
        assertThat(data).isInstanceOf(String.class);
        assertThat((String) data).isEqualTo(imageUrl);
    }

    @Test
    @DisplayName("로컬 업로드 경로(/uploads/...) 는 디스크에서 바이트를 읽어 Media 에 인라인된다 (base64 변환)")
    void should_inline_bytes_from_disk_when_local_upload_path(@TempDir Path tempDir) throws IOException {
        // given — 가짜 이미지 바이트를 ./uploads/portraits/upload-test.jpg 자리에 둔다
        Path portraitsDir = tempDir.resolve("portraits");
        Files.createDirectories(portraitsDir);
        byte[] fakeJpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x10, 0x20, 0x30};
        Files.write(portraitsDir.resolve("upload-test.jpg"), fakeJpegBytes);

        String imageUrl = "/uploads/portraits/upload-test.jpg";
        when(chatModel.call(any(Prompt.class))).thenReturn(fakeResponse("ok"));

        // when — uploadBaseDir 을 TempDir 로 주입해서 호출
        sut(tempDir.toString()).describe(imageUrl, "이 셀카 어떄?");

        // then — Media 의 data 는 byte[] (디스크에서 읽은 실제 바이트) 이어야 한다
        UserMessage userMessage = captureUserMessage();
        Object data = userMessage.getMedia().get(0).getData();
        assertThat(data).isInstanceOf(byte[].class);
        assertThat((byte[]) data).isEqualTo(fakeJpegBytes);
    }

    private UserMessage captureUserMessage() {
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        return (UserMessage) captor.getValue().getInstructions().get(0);
    }

    private ChatResponse fakeResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
