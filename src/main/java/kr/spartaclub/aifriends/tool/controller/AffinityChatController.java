package kr.spartaclub.aifriends.tool.controller;

import jakarta.validation.Valid;
import kr.spartaclub.aifriends.common.response.ApiResponse;
import kr.spartaclub.aifriends.tool.dto.AffinityChatRequest;
import kr.spartaclub.aifriends.tool.dto.AffinityChatResponse;
import kr.spartaclub.aifriends.tool.service.AffinityChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 11 Step 4 — 호감도 조회 도구를 활용한 자연어 대화 컨트롤러.
 *
 * <p>Step 2/3 의 컨트롤러는 그대로 두고, 시나리오 단위로 컨트롤러를 분리한다 —
 * "이 컨트롤러는 affinity 도구만 쓴다 → ChatClient 빈도 affinityChatClient 만 주입한다"
 * 가 한눈에 보이도록 결을 맞춘다.</p>
 */
@RestController
@RequestMapping("/api/tool/affinity")
public class AffinityChatController {

    private final AffinityChatService service;

    public AffinityChatController(AffinityChatService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AffinityChatResponse>> chat(
            @Valid @RequestBody AffinityChatRequest request
    ) {
        AffinityChatResponse response = service.chat(request.soulmateId(), request.message());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
