package kr.spartaclub.aifriends.practice;

import kr.spartaclub.aifriends.practice.dto.ActivityResponse;
import kr.spartaclub.aifriends.practice.dto.PostResponse;
import kr.spartaclub.aifriends.practice.dto.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PracticeController 슬라이스 테스트
 * 컨트롤러 + MVC 계층만 로드하고, PracticeService는 MockitoBean으로 대체해 외부 API 호출 없이 검증.
 */
@WebMvcTest(PracticeController.class)
@DisplayName("PracticeController 테스트")
class PracticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PracticeService practiceService;

    @Nested
    @DisplayName("GET /api/practice/post/{id}")
    class GetPost {

        @Test
        @DisplayName("200 OK와 ApiResponse(success, data)를 반환한다")
        void returnsPostWrappedInApiResponse() throws Exception {
            PostResponse post = new PostResponse(1, 1, "test title", "test body");
            given(practiceService.getPost(1L)).willReturn(post);

            mockMvc.perform(get("/api/practice/post/1").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.title").value("test title"))
                    .andExpect(jsonPath("$.data.body").value("test body"))
                    .andExpect(jsonPath("$.error").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /api/practice/user/{id}")
    class GetUser {

        @Test
        @DisplayName("200 OK와 ApiResponse(success, data)를 반환한다")
        void returnsUserWrappedInApiResponse() throws Exception {
            UserResponse.AddressResponse address = new UserResponse.AddressResponse(
                    "street", "suite", "서울", "10000",
                    new UserResponse.GeoResponse("37.5", "127.0"));
            UserResponse.CompanyResponse company = new UserResponse.CompanyResponse("회사", "문구", "bs");
            UserResponse user = new UserResponse(1, "홍길동", "hong", "hong@example.com", address, "010-0000-0000", "https://example.com", company);
            given(practiceService.getUser(1L)).willReturn(user);

            mockMvc.perform(get("/api/practice/user/1").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("홍길동"))
                    .andExpect(jsonPath("$.data.username").value("hong"))
                    .andExpect(jsonPath("$.data.email").value("hong@example.com"))
                    .andExpect(jsonPath("$.error").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /api/practice/activity")
    class GetActivity {

        @Test
        @DisplayName("200 OK와 ApiResponse(success, data)를 반환한다")
        void returnsActivityWrappedInApiResponse() throws Exception {
            ActivityResponse activity = new ActivityResponse(
                    "Learn Express.js", 0.25, "education", 1, 0.1,
                    "Few to no challenges", "hours", true, "https://expressjs.com/", "3943506");
            given(practiceService.getActivity()).willReturn(activity);

            mockMvc.perform(get("/api/practice/activity").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.activity").value("Learn Express.js"))
                    .andExpect(jsonPath("$.data.type").value("education"))
                    .andExpect(jsonPath("$.data.participants").value(1))
                    .andExpect(jsonPath("$.data.kidFriendly").value(true))
                    .andExpect(jsonPath("$.error").doesNotExist());
        }
    }
}
