package kr.spartaclub.aifriends.controller;

import jakarta.validation.Valid;
import kr.spartaclub.aifriends.common.response.ApiResponse;
import kr.spartaclub.aifriends.dto.AiChatRequest;
import kr.spartaclub.aifriends.dto.AiChatResponse;
import kr.spartaclub.aifriends.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트엔드와 실제로 통신하는 최상단 웹 계층 컨트롤러입니다.
 * 실시간 AI 채팅(대화 핑퐁)과 관련된 HTTP 진입점을 제공합니다.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    /**
     * API: POST /api/chat
     * AI 이성친구와 클라이언트 간의 메시지 송수신을 처리합니다.
     * REST 원칙상 새로운 채팅 기록(리소스)을 '생성'하는 행위이므로 POST 메서드가 적합합니다.
     *
     * @param request 프론트엔드에서 날아온 JSON 데이터 (이성친구 ID, 사용자 메시지)
     * @return 200 OK 와 함께 응답 데이터(AI 메시지, 갱신된 호감도 등)를 반환
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(@Valid @RequestBody AiChatRequest request) {
        
        // 비즈니스 로직(퍼사드) 호출
        AiChatResponse responseData = aiChatService.processChat(request);
        
        // 정해진 공통 응답 규격(ApiResponse)에 담아 200 OK로 반환
        return ResponseEntity.ok(ApiResponse.success(responseData));
    }
}
