package kr.spartaclub.aifriends.chat.controller;

import kr.spartaclub.aifriends.chat.privacy.UserAnonymizer;
import kr.spartaclub.aifriends.chat.service.SoulmateChatService;
import kr.spartaclub.aifriends.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 3 Step 3 — 소꿉친구 페르소나 엔드포인트.
 *
 * <p>유저 식별자는 UserAnonymizer 를 거쳐 별칭으로 치환된 뒤에만 서비스 계층으로 내려간다.
 * 덕분에 LLM 으로 흘러가는 프롬프트에 실명이 꽂힐 경로가 컨트롤러에서부터 차단된다.</p>
 *
 * <p>응답은 {@code ApiResponse&lt;SoulmateChatResponse&gt;} 로 감싸 CLAUDE.md 4-1 게이트와 정합.</p>
 */
@RestController
public class SoulmateChatController {

    private final SoulmateChatService service;
    private final UserAnonymizer userAnonymizer;

    public SoulmateChatController(SoulmateChatService service, UserAnonymizer userAnonymizer) {
        this.service = service;
        this.userAnonymizer = userAnonymizer;
    }

    @GetMapping("/api/chat/soulmate")
    public ResponseEntity<ApiResponse<SoulmateChatResponse>> soulmate(
            @RequestParam Long userId,
            @RequestParam String mood,
            @RequestParam String message
    ) {
        String anonymizedName = userAnonymizer.anonymize(userId);
        String aiMessage = service.chat(anonymizedName, mood, message);
        return ResponseEntity.ok(ApiResponse.success(new SoulmateChatResponse(aiMessage)));
    }
}
