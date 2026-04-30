package kr.spartaclub.aifriends.tool.controller;

import jakarta.validation.Valid;
import kr.spartaclub.aifriends.common.response.ApiResponse;
import kr.spartaclub.aifriends.tool.dto.ToolChatRequest;
import kr.spartaclub.aifriends.tool.dto.ToolChatResponse;
import kr.spartaclub.aifriends.tool.service.WeatherToolChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 11 Step 2 — Tool Calling 의 첫 번째 데모 컨트롤러.
 *
 * <p>학생은 도시명을 POST 본문에 담아 호출하고, LLM 이 등록된 {@code @Tool} 함수를
 * 자동 호출해 만든 캐릭터 응답을 받는다. 호출 흐름의 본질은 {@code WeatherToolChatService}
 * 에 있다 — 컨트롤러는 입력 검증과 ApiResponse 래핑만 책임진다 (§4-1).</p>
 */
@RestController
public class ToolCallingController {

    private final WeatherToolChatService service;

    public ToolCallingController(WeatherToolChatService service) {
        this.service = service;
    }

    @PostMapping("/api/tool/weather-chat")
    public ResponseEntity<ApiResponse<ToolChatResponse>> weatherChat(
            @Valid @RequestBody ToolChatRequest request
    ) {
        ToolChatResponse response = service.chat(request.city());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
