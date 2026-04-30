package kr.spartaclub.aifriends.chat.controller;

import kr.spartaclub.aifriends.chat.dto.AiReply;
import kr.spartaclub.aifriends.chat.dto.SoulmateChatResponse;
import kr.spartaclub.aifriends.chat.dto.SoulmateSessionMessageView;
import kr.spartaclub.aifriends.chat.privacy.UserAnonymizer;
import kr.spartaclub.aifriends.chat.service.SoulmateChatService;
import kr.spartaclub.aifriends.common.response.ApiResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

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

    /**
     * Day 6 Step 2~3 — 토큰 단위 스트리밍 응답 엔드포인트.
     *
     * <p>{@code produces = MediaType.TEXT_EVENT_STREAM_VALUE} 로 SSE 임을 명시하면
     * Spring MVC 의 {@code ReactiveTypeHandler} 가 컨트롤러가 반환한 {@code Flux<String>}
     * 을 자동으로 {@code ResponseBodyEmitter} 로 변환해 토큰을 흘려준다.</p>
     *
     * <p>SSE 응답은 §4-1 ApiResponse 래핑 규약의 정당한 예외다. 청크 단위로 흐르는
     * {@code text/event-stream} 본문에 JSON wrapper 를 끼워 넣으면 스트리밍 의미가 깨진다.
     * 에러 처리는 {@code Flux.onErrorResume(...)} 같은 Reactor 연산으로 흐름 안에서 처리한다.</p>
     *
     * <p>Day 6 Step 5 에서 ChatMemory 통합이 들어오면 conversationId 파라미터가 추가되고,
     * {@code Flux.doOnComplete()} 으로 스트림이 끝난 뒤 일괄 저장하는 경로가 붙는다.</p>
     */
    @GetMapping(value = "/api/chat/soulmate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam Long userId,
            @RequestParam String mood,
            @RequestParam String message
    ) {
        String anonymizedName = userAnonymizer.anonymize(userId);
        return service.chatStream(anonymizedName, mood, message);
    }
}
