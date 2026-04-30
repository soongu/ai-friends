package kr.spartaclub.aifriends.tool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.spartaclub.aifriends.tool.dto.GameRecallRequest;
import kr.spartaclub.aifriends.tool.dto.GameRecallResponse;
import kr.spartaclub.aifriends.tool.dto.GameSaveRequest;
import kr.spartaclub.aifriends.tool.service.GameStateRecallService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Day 11 Step 3 — {@link GameStateRecallController} 슬라이스 테스트.
 *
 * <p>Service 레이어를 모킹해 컨트롤러의 입출력 계약(ApiResponse 래핑 + 입력 검증) 만 검증한다.
 * 실제 ChatClient · @Tool 디스패치 검증은 강사 수동 smoke 의 책임.</p>
 */
@WebMvcTest(GameStateRecallController.class)
class GameStateRecallControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GameStateRecallService service;

    @Test
    @DisplayName("POST /api/tool/game/save - 정상 요청이면 ApiResponse.success 와 안내 메시지를 내려준다")
    void save_returnsAck() throws Exception {
        GameSaveRequest request = new GameSaveRequest(42L, "오늘 힘들었어", "내가 옆에 있을게", 7);

        mockMvc.perform(post("/api/tool/game/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").value(42))
                .andExpect(jsonPath("$.data.turnCount").value(7));

        then(service).should().save(42L, "오늘 힘들었어", "내가 옆에 있을게", 7);
    }

    @Test
    @DisplayName("POST /api/tool/game/save - playerId 가 null 이면 400 + ApiResponse.fail 을 반환한다")
    void save_nullPlayerId_returns400() throws Exception {
        GameSaveRequest request = new GameSaveRequest(null, "x", "y", 1);

        mockMvc.perform(post("/api/tool/game/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("E001"));

        then(service).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /api/tool/game/recall - LLM 의 회상 응답을 ApiResponse 로 감싸 내려준다")
    void recall_returnsCharacterReply() throws Exception {
        given(service.recall(anyLong())).willReturn(new GameRecallResponse(
                42L,
                "어, 우리 저번에 7턴쯤 얘기하다 멈췄지? 그때 너 힘들다고 했었잖아. 지금은 좀 어때?"));

        GameRecallRequest request = new GameRecallRequest(42L);

        mockMvc.perform(post("/api/tool/game/recall")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").value(42))
                .andExpect(jsonPath("$.data.aiMessage").value(
                        "어, 우리 저번에 7턴쯤 얘기하다 멈췄지? 그때 너 힘들다고 했었잖아. 지금은 좀 어때?"));

        then(service).should().recall(42L);
    }

    @Test
    @DisplayName("POST /api/tool/game/recall - playerId 가 null 이면 400 + ApiResponse.fail 을 반환한다")
    void recall_nullPlayerId_returns400() throws Exception {
        GameRecallRequest request = new GameRecallRequest(null);

        mockMvc.perform(post("/api/tool/game/recall")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("E001"));

        then(service).should(never()).recall(anyLong());
    }
}
