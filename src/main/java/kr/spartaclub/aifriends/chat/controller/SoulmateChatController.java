package kr.spartaclub.aifriends.chat.controller;

import kr.spartaclub.aifriends.chat.dto.AiReply;
import kr.spartaclub.aifriends.chat.privacy.UserAnonymizer;
import kr.spartaclub.aifriends.chat.service.SoulmateChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 3 Step 3 — 소꿉친구 페르소나 엔드포인트.
 *
 * <p>유저 식별자는 UserAnonymizer 를 거쳐 별칭으로 치환된 뒤에만 서비스 계층으로 내려간다.
 * 덕분에 LLM 으로 흘러가는 프롬프트에 실명이 꽂힐 경로가 컨트롤러에서부터 차단된다.</p>
 *
 * <p>Day 4 Step 5 — 응답 타입을 평문 {@code String} 에서 {@link AiReply} record 로 교체했다.
 * Spring MVC 가 record 를 자동으로 JSON 직렬화하므로 응답이 구조화된 객체로 클라이언트에 전달된다.
 * (ApiResponse 래핑은 같은 Day 의 후속 fix 커밋에서 일괄 적용된다.)</p>
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
    public AiReply soulmate(
            @RequestParam Long userId,
            @RequestParam String mood,
            @RequestParam String message
    ) {
        String anonymizedName = userAnonymizer.anonymize(userId);
        return service.chat(anonymizedName, mood, message);
    }
}
