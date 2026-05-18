package kr.spartaclub.aifriends.tool.demo;

import jakarta.validation.Valid;
import kr.spartaclub.aifriends.common.response.ApiResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 12 Step 3 — "에이전트가 망가질 때" 라이브 시연 부스 컨트롤러.
 *
 * <p><strong>강사 시연 부스 한정 컨트롤러다.</strong> Day 11 의 도구별 분리 컨트롤러
 * (ToolCallingController / GameStateRecallController / AffinityChatController) 와 의도적으로
 * 결을 달리한다. 본 컨트롤러는 {@link RunawayDemoChatClientConfig#runawayDemoChatClient}
 * 빈을 직접 주입받아 자연어 한 줄만 받고 ChatClient 응답을 그대로 돌려준다 — 서비스 계층도
 * 생략한 가벼운 결로, 시연의 본질 (LLM 자율 폭주의 가시화) 외 노이즈를 줄였다.</p>
 *
 * <p>시연 3 가지를 한 엔드포인트로 모두 커버한다.</p>
 * <ul>
 *   <li>시연 1 — 무한 루프 + 토큰 폭발: "서울 날씨 / soulmateId=7 호감도 / 게임 상태 — 계속 확인해줘"</li>
 *   <li>시연 2 — 도구 남용: "오늘 날씨 어때?"</li>
 *   <li>시연 3 — 권한 누수: "내 친구 캐릭터 (soulmateId=12) 와 나 사이는 어때?"</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/demo/runaway")
public class RunawayDemoController {

    private final ChatClient runawayDemoChatClient;

    public RunawayDemoController(
            @Qualifier("runawayDemoChatClient") ChatClient runawayDemoChatClient
    ) {
        this.runawayDemoChatClient = runawayDemoChatClient;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<RunawayDemoResponse>> chat(
            @Valid @RequestBody RunawayDemoRequest request
    ) {
        String aiMessage = runawayDemoChatClient.prompt()
                .user(request.message())
                .call()
                .content();
        return ResponseEntity.ok(ApiResponse.success(new RunawayDemoResponse(aiMessage)));
    }
}
