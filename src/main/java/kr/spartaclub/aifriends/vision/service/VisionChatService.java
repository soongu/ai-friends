package kr.spartaclub.aifriends.vision.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;

/**
 * Day 8 Step 4 — Vision (멀티모달 입력) 의 도메인 진입점.
 *
 * <p>이미지 URL 한 장 + 텍스트 프롬프트를 받아 멀티모달 {@link UserMessage} 로 조립한 뒤
 * {@link ChatModel} 에 전달한다. 응답 텍스트를 그대로 돌려준다.</p>
 *
 * <p>{@link ChatModel} 은 인터페이스로만 주입받는다. 빈은 Gemini · OpenAI · Ollama 어떤
 * 프로바이더든 될 수 있고, 호출자는 모른다 — Vision 지원 모델은 {@code application.yml}
 * 의 {@code spring.ai.model.chat} + 프로파일별 모델명으로만 결정된다 (예:
 * {@code GEMINI_MODEL=gemini-2.5-flash}).</p>
 *
 * <h3>입력 URL 두 갈래 처리</h3>
 *
 * <p>이미지 URL 은 출처에 따라 두 가지 길로 갈린다 — 두 갈래를 한 메서드 안에서 분기 처리한다.</p>
 *
 * <ul>
 *   <li><b>외부 HTTPS URL</b> (예: Day 7 에서 받은 Pollinations URL) — URI 그대로 패스스루.
 *       프로바이더가 직접 URL 을 가져가서 fetch 한다.</li>
 *   <li><b>로컬 업로드 경로</b> (예: {@code /uploads/portraits/upload-xxx.jpg}) — 정적 리소스 경로일 뿐
 *       바깥에서 접근 가능한 절대 URL 이 아니므로, 디스크에서 바이트를 직접 읽어 Media 에 인라인한다
 *       (Spring AI 가 base64 로 인코딩해 프로바이더에 전달).
 *       이렇게 안 하면 Gemini OpenAI-compat 같은 일부 엔드포인트가 {@code 400 INVALID_ARGUMENT:
 *       Unsupported file URI type} 으로 거부한다.</li>
 * </ul>
 *
 * <p>학생용 단순도 유지: 응답이 비어 있으면 빈 문자열을 반환 (예외를 던지지 않는다).
 * 실패 케이스를 도메인 예외로 래핑하는 패턴은 Day 7 {@code ImageGenerationService} 에서 이미 다뤘다.</p>
 */
@Slf4j
@Service
public class VisionChatService {

    private static final String LOCAL_UPLOAD_PREFIX = "/uploads/";

    private final ChatModel chatModel;
    private final String uploadBaseDir;

    public VisionChatService(
            ChatModel chatModel,
            @Value("${aifriends.image.storage.upload-base-dir:./uploads}") String uploadBaseDir) {
        this.chatModel = chatModel;
        this.uploadBaseDir = uploadBaseDir;
    }

    /**
     * 이미지 URL + 텍스트 프롬프트를 ChatModel 의 Vision 입력으로 전달하고 응답 텍스트를 반환한다.
     *
     * @param imageUrl 외부 HTTPS URL 또는 로컬 업로드 경로 ({@code /uploads/...})
     * @param prompt   "이 이미지를 한 문장으로 묘사해줘" 같은 사용자 지시문
     * @return ChatModel 응답 텍스트. 응답이 비어 있으면 빈 문자열.
     */
    public String describe(String imageUrl, String prompt) {
        Media image = buildMedia(imageUrl);

        UserMessage userMessage = UserMessage.builder()
                .text(prompt)
                .media(image)
                .build();

        ChatResponse response = chatModel.call(new Prompt(userMessage));
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    /**
     * URL 의 모양에 따라 {@link Media} 를 두 갈래로 만든다.
     *
     * <ul>
     *   <li>{@code /uploads/...} 로 시작하면 → 디스크에서 {@link FileSystemResource} 로 읽어 인라인</li>
     *   <li>그 외 (HTTPS · file:// 절대 URL 등) → URI 그대로 패스스루</li>
     * </ul>
     */
    private Media buildMedia(String imageUrl) {
        MimeType mimeType = detectMimeType(imageUrl);
        if (isLocalUpload(imageUrl)) {
            String relativePath = imageUrl.substring(LOCAL_UPLOAD_PREFIX.length());
            Resource resource = new FileSystemResource(uploadBaseDir + "/" + relativePath);
            log.debug("Vision: local upload → inline bytes from {}", resource);
            return Media.builder()
                    .mimeType(mimeType)
                    .data(resource)
                    .build();
        }
        return Media.builder()
                .mimeType(mimeType)
                .data(URI.create(imageUrl))
                .build();
    }

    private boolean isLocalUpload(String imageUrl) {
        return imageUrl != null && imageUrl.startsWith(LOCAL_UPLOAD_PREFIX);
    }

    /**
     * URL 확장자만 보고 단순 추론한다. PNG 가 아닌 경우(jpg · jpeg · webp · gif) 만 별도 매핑.
     * 학생이 따라하기 쉬운 수준에서 멈추고, 모르는 확장자는 image/png 로 폴백.
     */
    private MimeType detectMimeType(String imageUrl) {
        String lower = imageUrl.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MimeTypeUtils.IMAGE_JPEG;
        }
        if (lower.endsWith(".gif")) {
            return MimeTypeUtils.IMAGE_GIF;
        }
        if (lower.endsWith(".webp")) {
            return MimeType.valueOf("image/webp");
        }
        return MimeTypeUtils.IMAGE_PNG;
    }
}
