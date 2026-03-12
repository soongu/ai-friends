package kr.spartaclub.aifriends;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 전체 API를 실제 스프링 컨텍스트와 빈(Controller, Service, Repository 등)으로 통합 테스트합니다.
 * MockMvc로 HTTP 요청을 보내고, 응답 상태·본문을 검증합니다. (TestRestTemplate 없이 동일한 통합 검증)
 *
 * <ul>
 *   <li>Practice API: JSONPlaceholder, Bored API를 실제로 호출합니다.</li>
 *   <li>Soulmate/Chat API: H2 인메모리 DB를 사용합니다.</li>
 *   <li>POST /api/chat: Gemini API를 호출합니다. GEMINI_API_KEY가 없으면 401/502 등으로 실패할 수 있습니다.</li>
 * </ul>
 */
@SpringBootTest(
        properties = {"DB_URL=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"}
)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long createdSoulmateId;

    private Map<String, Object> parseApiResponse(ResultActions result) throws Exception {
        String json = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    // ==================== Practice API ====================

    @Test
    @Order(1)
    @DisplayName("GET /api/practice/post/1 - JSONPlaceholder 게시글 조회")
    void practiceGetPost() throws Exception {
        ResultActions actions = mockMvc.perform(get("/api/practice/post/1").accept(MediaType.APPLICATION_JSON));
        actions.andExpect(status().isOk());
        Map<String, Object> body = parseApiResponse(actions);
        assertThat(body.get("success")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("id")).isNotNull();
        assertThat(data.get("title")).isNotNull();
        assertThat(data.get("body")).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/practice/user/1 - JSONPlaceholder 사용자 조회")
    void practiceGetUser() throws Exception {
        ResultActions actions = mockMvc.perform(get("/api/practice/user/1").accept(MediaType.APPLICATION_JSON));
        actions.andExpect(status().isOk());
        Map<String, Object> body = parseApiResponse(actions);
        assertThat(body.get("success")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("id")).isNotNull();
        assertThat(data.get("name")).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/practice/activity - Bored API 랜덤 활동 조회")
    void practiceGetActivity() throws Exception {
        ResultActions actions = mockMvc.perform(get("/api/practice/activity").accept(MediaType.APPLICATION_JSON));
        actions.andExpect(status().isOk());
        Map<String, Object> body = parseApiResponse(actions);
        assertThat(body.get("success")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("activity")).isNotNull();
    }

    // ==================== Soulmate API ====================

    @Test
    @Order(10)
    @DisplayName("POST /api/soulmates - 이성친구 생성")
    void soulmateCreate() throws Exception {
        Map<String, Object> req = Map.of(
                "gender", "FEMALE",
                "characterImageId", "img-001",
                "characterImageUrl", "https://example.com/img.png",
                "name", "테스트캐릭터",
                "personalityKeywords", List.of("친절한", "유머러스한"),
                "hobbies", List.of("독서", "영화"),
                "speechStyles", List.of("반말", "다정한")
        );
        ResultActions actions = mockMvc.perform(
                post("/api/soulmates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
        );
        actions.andExpect(status().isCreated());
        Map<String, Object> body = parseApiResponse(actions);
        assertThat(body.get("success")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("id")).isNotNull();
        createdSoulmateId = ((Number) data.get("id")).longValue();
        String location = actions.andReturn().getResponse().getHeader("Location");
        assertThat(location).contains("/api/soulmates/" + createdSoulmateId);
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/soulmates - 이성친구 목록 조회")
    void soulmateList() throws Exception {
        ResultActions actions = mockMvc.perform(get("/api/soulmates").accept(MediaType.APPLICATION_JSON));
        actions.andExpect(status().isOk());
        Map<String, Object> body = parseApiResponse(actions);
        assertThat(body.get("success")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("soulmates")).isInstanceOf(List.class);
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/soulmates/{id} - 이성친구 프로필 조회")
    void soulmateGet() throws Exception {
        Assumptions.assumeTrue(createdSoulmateId != null, "soulmateCreate()가 먼저 실행되어야 합니다.");
        ResultActions actions = mockMvc.perform(
                get("/api/soulmates/" + createdSoulmateId).accept(MediaType.APPLICATION_JSON));
        actions.andExpect(status().isOk());
        Map<String, Object> body = parseApiResponse(actions);
        assertThat(body.get("success")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("id")).isEqualTo(createdSoulmateId);
        assertThat(data.get("affectionScore")).isNotNull();
        assertThat(data.get("level")).isNotNull();
    }

    @Test
    @Order(13)
    @DisplayName("GET /api/soulmates/{id}/chat/logs - 대화 기록 Slice 조회")
    void soulmateChatLogs() throws Exception {
        Assumptions.assumeTrue(createdSoulmateId != null, "soulmateCreate()가 먼저 실행되어야 합니다.");
        ResultActions actions = mockMvc.perform(
                get("/api/soulmates/" + createdSoulmateId + "/chat/logs").param("size", "10")
                        .accept(MediaType.APPLICATION_JSON));
        actions.andExpect(status().isOk());
        Map<String, Object> body = parseApiResponse(actions);
        assertThat(body.get("success")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("content")).isInstanceOf(List.class);
    }

    // ==================== AiChat API ====================

    @Test
    @Order(20)
    @DisplayName("POST /api/chat - AI 채팅 (Gemini API 호출, GEMINI_API_KEY 필요)")
    void chatPost() throws Exception {
        Assumptions.assumeTrue(createdSoulmateId != null, "soulmateCreate()가 먼저 실행되어야 합니다.");
        Map<String, Object> req = Map.of(
                "soulmateId", createdSoulmateId,
                "userMessage", "안녕!"
        );
        ResultActions actions = mockMvc.perform(
                post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
        );
        int status = actions.andReturn().getResponse().getStatus();
        assertThat(status).isIn(200, 400, 401, 429, 502);
        Map<String, Object> body = parseApiResponse(actions);
        assertThat(body).isNotNull();
        if (status == 200) {
            assertThat(body.get("success")).isEqualTo(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            assertThat(data).isNotNull();
            assertThat(data.get("aiMessage")).isNotNull();
        }
    }
}
