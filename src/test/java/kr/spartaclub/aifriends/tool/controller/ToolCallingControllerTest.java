package kr.spartaclub.aifriends.tool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.spartaclub.aifriends.tool.dto.ToolChatRequest;
import kr.spartaclub.aifriends.tool.dto.ToolChatResponse;
import kr.spartaclub.aifriends.tool.service.WeatherToolChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Day 11 Step 2 — Tool Calling 컨트롤러 슬라이스 테스트.
 *
 * <p>Service 레이어를 {@code @MockBean} 으로 stub 처리해 컨트롤러의 입출력 계약만 검증한다.
 * ChatClient · 실제 LLM 호출 · @Tool 디스패치는 Service 단위 테스트와 강사 수동 smoke 의 책임.</p>
 *
 * <p>요청 본문 검증(빈 도시명) 은 GlobalExceptionHandler 가
 * {@code MethodArgumentNotValidException} 을 ApiResponse.fail 로 변환해주는 결과와 정합을 맞춘다.</p>
 */
@WebMvcTest(ToolCallingController.class)
class ToolCallingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WeatherToolChatService service;

    @Test
    @DisplayName("POST /api/tool/weather-chat - 정상 요청이면 ApiResponse.success 로 캐릭터 응답을 내려준다")
    void weatherChat_returnsCharacterReply() throws Exception {
        given(service.chat(eq("서울"))).willReturn(
                new ToolChatResponse(
                        "서울",
                        "오늘 서울은 흐리고 23도래! 가벼운 가디건 하나 챙겨가자~",
                        "흐림",
                        23,
                        60));

        ToolChatRequest request = new ToolChatRequest("서울");

        mockMvc.perform(post("/api/tool/weather-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.city").value("서울"))
                .andExpect(jsonPath("$.data.aiMessage").value(
                        "오늘 서울은 흐리고 23도래! 가벼운 가디건 하나 챙겨가자~"))
                .andExpect(jsonPath("$.data.condition").value("흐림"))
                .andExpect(jsonPath("$.data.temperatureCelsius").value(23))
                .andExpect(jsonPath("$.data.precipitationChance").value(60));

        then(service).should().chat("서울");
    }

    @Test
    @DisplayName("POST /api/tool/weather-chat - city 가 빈 문자열이면 400 과 ApiResponse.fail 을 반환한다")
    void weatherChat_blankCity_returns400() throws Exception {
        ToolChatRequest request = new ToolChatRequest("");

        mockMvc.perform(post("/api/tool/weather-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("E001"));

        then(service).should(never()).chat(anyString());
    }
}
