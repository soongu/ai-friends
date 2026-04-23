package kr.spartaclub.aifriends.chat.controller;

import kr.spartaclub.aifriends.chat.service.SoulmateChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 3 Step 2 — 소꿉친구 페르소나 엔드포인트.
 *
 * <p>컨트롤러·서비스 어디에도 "너는 소꿉친구야..." 문자열이 없다.
 * 페르소나는 ChatClientConfig 의 defaultSystem 에만 존재한다.</p>
 */
@RestController
public class SoulmateChatController {

    private final SoulmateChatService service;

    public SoulmateChatController(SoulmateChatService service) {
        this.service = service;
    }

    @GetMapping("/api/chat/soulmate")
    public String soulmate(@RequestParam String message) {
        return service.chat(message);
    }
}
