package kr.spartaclub.aifriends.chat.controller;

import kr.spartaclub.aifriends.chat.dto.AiReply;
import kr.spartaclub.aifriends.chat.dto.SoulmateChatResponse;
import kr.spartaclub.aifriends.chat.dto.SoulmateSessionMessageView;
import kr.spartaclub.aifriends.chat.privacy.UserAnonymizer;
import kr.spartaclub.aifriends.chat.service.SoulmateChatService;
import kr.spartaclub.aifriends.common.response.ApiResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Day 3 Step 3 — 소꿉친구 페르소나 엔드포인트.
 *
 * <p>유저 식별자는 UserAnonymizer 를 거쳐 별칭으로 치환된 뒤에만 서비스 계층으로 내려간다.
 * 덕분에 LLM 으로 흘러가는 프롬프트에 실명이 꽂힐 경로가 컨트롤러에서부터 차단된다.</p>
 *
 * <p>Day 4 Step 5 — 응답 타입을 평문 {@code String} 에서 {@link AiReply} record 로 교체하고,
 * 표준 응답 래퍼 {@link ApiResponse} 로 감싸 GlobalExceptionHandler 의 에러 응답 형태와 정합을 맞춘다.</p>
 *
 * <p>Day 5 Step 5 — conversationId 파라미터로 세션을 분리한다. 첫 호출 시 클라이언트가
 * 비워두면 서버가 UUID 를 발급해 응답에 함께 내려주고, 두 번째 호출부터는 클라이언트가
 * 그대로 들고 와야 멀티턴 이력이 이어진다. 세션 조회·삭제 엔드포인트도 함께 추가했다.</p>
 */
@RestController
public class SoulmateChatController {

    private final SoulmateChatService service;
    private final UserAnonymizer userAnonymizer;
    private final ChatMemory chatMemory;

    public SoulmateChatController(SoulmateChatService service,
                                  UserAnonymizer userAnonymizer,
                                  ChatMemory chatMemory) {
        this.service = service;
        this.userAnonymizer = userAnonymizer;
        this.chatMemory = chatMemory;
    }

    @GetMapping("/api/chat/soulmate")
    public ResponseEntity<ApiResponse<SoulmateChatResponse>> soulmate(
            @RequestParam Long userId,
            @RequestParam String mood,
            @RequestParam String message,
            @RequestParam(required = false) String conversationId
    ) {
        String anonymizedName = userAnonymizer.anonymize(userId);
        String convId = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;
        AiReply reply = service.chat(convId, anonymizedName, mood, message);
        return ResponseEntity.ok(ApiResponse.success(new SoulmateChatResponse(convId, reply)));
    }

    @GetMapping("/api/chat/soulmate/sessions/{conversationId}")
    public ResponseEntity<ApiResponse<List<SoulmateSessionMessageView>>> getSession(
            @PathVariable String conversationId
    ) {
        List<Message> messages = chatMemory.get(conversationId);
        List<SoulmateSessionMessageView> views = messages.stream()
                .map(m -> new SoulmateSessionMessageView(
                        m.getMessageType().name().toLowerCase(),
                        m.getText()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(views));
    }

    @DeleteMapping("/api/chat/soulmate/sessions/{conversationId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable String conversationId) {
        chatMemory.clear(conversationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
