package kr.spartaclub.aifriends.controller;

import kr.spartaclub.aifriends.common.exception.GlobalExceptionHandler;
import kr.spartaclub.aifriends.domain.ChatLog;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.dto.ChatLogResponse;
import kr.spartaclub.aifriends.dto.SoulmateCreateRequest;
import kr.spartaclub.aifriends.dto.SoulmateProfileResponse;
import kr.spartaclub.aifriends.dto.SoulmateResponse;
import kr.spartaclub.aifriends.service.ChatLogService;
import kr.spartaclub.aifriends.service.SoulmateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SoulmateController.class)
@Import(GlobalExceptionHandler.class)
class SoulmateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SoulmateService soulmateService;

    @MockitoBean
    private ChatLogService chatLogService;

    @Test
    @DisplayName("이성친구 생성 성공")
    void createSoulmate_success() throws Exception {
        // given
        SoulmateCreateRequest request = new SoulmateCreateRequest(
                "FEMALE", "img1", "url", "Alice",
                List.of("kind"), List.of("reading"), List.of("gentle")
        );
        Soulmate entity = new Soulmate(1L, "FEMALE", "img1", "url", "Alice", "kind", "reading", "gentle", 0, 1, java.time.LocalDateTime.now());
        SoulmateResponse mockResponse = SoulmateResponse.from(entity);
        given(soulmateService.createSoulmate(any())).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/soulmates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Alice"));
    }

    @Test
    @DisplayName("이성친구 생성시 유효성 검사 실패 (성별 누락)")
    void createSoulmate_validationFail() throws Exception {
        // given
        SoulmateCreateRequest request = new SoulmateCreateRequest(
                "", "img1", "url", "Alice",
                List.of("kind"), List.of("reading"), List.of("gentle")
        );

        // when & then
        mockMvc.perform(post("/api/soulmates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("성별을 입력해 주세요"));
    }

    @Test
    @DisplayName("이성친구 프로필 상세 단건 조회 성공")
    void getSoulmateProfile_success() throws Exception {
        // given
        Soulmate entity = new Soulmate(1L, "MALE", "img1", null, "Bob", "kind", "none", "none", 0, 1, java.time.LocalDateTime.now());
        SoulmateProfileResponse mockResponse = SoulmateProfileResponse.of(entity, List.of("BADGE_1"));
        given(soulmateService.getSoulmate(1L)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/soulmates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.badges[0]").value("BADGE_1"));
    }

    @Test
    @DisplayName("채팅 내역 내역 페이징(Slice) 조회 성공")
    void getChatLogs_success() throws Exception {
        // given
        ChatLog log = new ChatLog(1L, 1L, "USER", "안녕", java.time.LocalDateTime.now());
        Slice<ChatLogResponse> slice = new SliceImpl<>(List.of(ChatLogResponse.from(log)));
        given(chatLogService.getChatLogs(eq(1L), any(Pageable.class))).willReturn(slice);

        // when & then
        mockMvc.perform(get("/api/soulmates/1/chat/logs?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].speaker").value("USER"))
                .andExpect(jsonPath("$.data.content[0].message").value("안녕"))
                .andExpect(jsonPath("$.data.last").value(true));
    }
}
