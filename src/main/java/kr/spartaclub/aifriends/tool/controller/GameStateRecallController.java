package kr.spartaclub.aifriends.tool.controller;

import jakarta.validation.Valid;
import kr.spartaclub.aifriends.common.response.ApiResponse;
import kr.spartaclub.aifriends.tool.dto.GameRecallRequest;
import kr.spartaclub.aifriends.tool.dto.GameRecallResponse;
import kr.spartaclub.aifriends.tool.dto.GameSaveRequest;
import kr.spartaclub.aifriends.tool.dto.GameSaveResponse;
import kr.spartaclub.aifriends.tool.service.GameStateRecallService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 11 Step 3 — 게임 상태 저장 / 회상 컨트롤러.
 *
 * <p>Step 2 의 {@link ToolCallingController} 는 그대로 두고, 도구 스코프 격리 결을 강조하기 위해
 * 별도 컨트롤러로 분리한다. 학생이 "이 컨트롤러는 game 도구만 쓴다 → ChatClient 빈도
 * gameStateChatClient 만 주입한다" 를 한눈에 잡을 수 있게 한다.</p>
 */
@RestController
@RequestMapping("/api/tool/game")
public class GameStateRecallController {

    private final GameStateRecallService service;

    public GameStateRecallController(GameStateRecallService service) {
        this.service = service;
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<GameSaveResponse>> save(
            @Valid @RequestBody GameSaveRequest request
    ) {
        service.save(request.playerId(), request.lastUserMessage(), request.lastAiMessage(), request.turnCount());
        return ResponseEntity.ok(ApiResponse.success(
                new GameSaveResponse(request.playerId(), request.turnCount())));
    }

    @PostMapping("/recall")
    public ResponseEntity<ApiResponse<GameRecallResponse>> recall(
            @Valid @RequestBody GameRecallRequest request
    ) {
        GameRecallResponse response = service.recall(request.playerId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
