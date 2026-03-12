package kr.spartaclub.aifriends.practice;

import kr.spartaclub.aifriends.practice.dto.ActivityResponse;
import kr.spartaclub.aifriends.practice.dto.PostResponse;
import kr.spartaclub.aifriends.practice.dto.UserResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PracticeService 단위 테스트.
 * 외부 API(JSONPlaceholder, Bored)는 MockWebServer로 대체해 실제 네트워크 호출 없이 검증.
 */
@DisplayName("PracticeService 단위 테스트")
class PracticeServiceTest {

    private MockWebServer jsonPlaceholderServer;
    private MockWebServer boredServer;

    @AfterEach
    void tearDown() throws IOException {
        if (jsonPlaceholderServer != null) {
            jsonPlaceholderServer.shutdown();
        }
        if (boredServer != null) {
            boredServer.shutdown();
        }
    }

    private PracticeService createService() throws IOException {
        jsonPlaceholderServer = new MockWebServer();
        jsonPlaceholderServer.start();
        boredServer = new MockWebServer();
        boredServer.start();

        RestClient jsonPlaceholderClient = RestClient.builder()
                .baseUrl(jsonPlaceholderServer.url("/").toString())
                .build();
        RestClient boredClient = RestClient.builder()
                .baseUrl(boredServer.url("/").toString())
                .build();
        return new PracticeService(jsonPlaceholderClient, boredClient);
    }

    @Nested
    @DisplayName("getPost")
    class GetPost {

        @Test
        @DisplayName("id로 게시글을 조회하면 PostResponse를 반환한다")
        void returnsPostResponse() throws Exception {
            PracticeService service = createService();
            String body = """
                    {"userId":1,"id":1,"title":"test title","body":"test body"}
                    """;
            jsonPlaceholderServer.enqueue(new MockResponse()
                    .setBody(body)
                    .addHeader("Content-Type", "application/json"));

            PostResponse result = service.getPost(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1);
            assertThat(result.userId()).isEqualTo(1);
            assertThat(result.title()).isEqualTo("test title");
            assertThat(result.body()).isEqualTo("test body");
        }
    }

    @Nested
    @DisplayName("getUser")
    class GetUser {

        @Test
        @DisplayName("id로 사용자를 조회하면 UserResponse를 반환한다")
        void returnsUserResponse() throws Exception {
            PracticeService service = createService();
            String body = """
                    {
                      "id":1,"name":"홍길동","username":"hong","email":"hong@example.com",
                      "address":{"street":"거리","suite":"101","city":"서울","zipcode":"10000","geo":{"lat":"37.5","lng":"127.0"}},
                      "phone":"010-0000-0000","website":"https://example.com",
                      "company":{"name":"회사","catchPhrase":"문구","bs":"bs"}
                    }
                    """;
            jsonPlaceholderServer.enqueue(new MockResponse()
                    .setBody(body)
                    .addHeader("Content-Type", "application/json"));

            UserResponse result = service.getUser(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1);
            assertThat(result.name()).isEqualTo("홍길동");
            assertThat(result.username()).isEqualTo("hong");
            assertThat(result.email()).isEqualTo("hong@example.com");
            assertThat(result.address()).isNotNull();
            assertThat(result.address().city()).isEqualTo("서울");
            assertThat(result.company()).isNotNull();
            assertThat(result.company().name()).isEqualTo("회사");
        }
    }

    @Nested
    @DisplayName("getActivity")
    class GetActivity {

        @Test
        @DisplayName("랜덤 활동 1건을 조회하면 ActivityResponse를 반환한다")
        void returnsActivityResponse() throws Exception {
            PracticeService service = createService();
            String body = """
                    {
                      "activity":"Learn Express.js",
                      "availability":0.25,
                      "type":"education",
                      "participants":1,
                      "price":0.1,
                      "accessibility":"Few to no challenges",
                      "duration":"hours",
                      "kidFriendly":true,
                      "link":"https://expressjs.com/",
                      "key":"3943506"
                    }
                    """;
            boredServer.enqueue(new MockResponse()
                    .setBody(body)
                    .addHeader("Content-Type", "application/json"));

            ActivityResponse result = service.getActivity();

            assertThat(result).isNotNull();
            assertThat(result.activity()).isEqualTo("Learn Express.js");
            assertThat(result.availability()).isEqualTo(0.25);
            assertThat(result.type()).isEqualTo("education");
            assertThat(result.participants()).isEqualTo(1);
            assertThat(result.price()).isEqualTo(0.1);
            assertThat(result.accessibility()).isEqualTo("Few to no challenges");
            assertThat(result.duration()).isEqualTo("hours");
            assertThat(result.kidFriendly()).isTrue();
            assertThat(result.link()).isEqualTo("https://expressjs.com/");
            assertThat(result.key()).isEqualTo("3943506");
        }
    }
}
