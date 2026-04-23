package kr.spartaclub.aifriends.service;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.ChatLog;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.dto.GeminiParsedResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiServiceTest {

    private MockWebServer mockWebServer;
    private GeminiService geminiService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        RestClient restClient = RestClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        geminiService = new GeminiService(restClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Gemini API 정상 응답 성공 - JSON 파싱 처리")
    void generateReply_success() throws Exception {
        // given
        Soulmate soulmate = new Soulmate(1L, "MALE", "img", null, null, "x", "y", "z", 0, 1, null);

        // Mock Gemini Response Structure
        String mockResponseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"aiMessage\\": \\"안녕!\\", \\"choices\\": [\\"선택1\\"], \\"affectionDelta\\": 2}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when
        GeminiParsedResponse response = geminiService.generateReply(soulmate, Collections.emptyList(), "hello", false, false);

        // then
        assertThat(response.aiMessage()).isEqualTo("안녕!");
        assertThat(response.choices()).containsExactly("선택1");
        assertThat(response.affectionDelta()).isEqualTo(2);
    }

    @Test
    @DisplayName("Gemini API 429 Rate Limit 에러 발생 시 커스텀 예외")
    void generateReply_rateLimit() {
        // given
        Soulmate soulmate = new Soulmate(1L, "MALE", "img", null, null, "x", "y", "z", 0, 1, null);
        mockWebServer.enqueue(new MockResponse().setResponseCode(429));

        // when & then
        assertThatThrownBy(() -> geminiService.generateReply(soulmate, Collections.emptyList(), "hi", false, false))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.RATE_LIMIT.getMessage());
    }

    @Test
    @DisplayName("Gemini JSON 응답 파싱 실패 시 원문 담기")
    void generateReply_parseFailureFallback() {
        Soulmate soulmate = new Soulmate(1L, "MALE", "img", null, null, "x", "y", "z", 0, 1, null);
        String mockResponseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "그냥 평범한 텍스트로 응답함"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        GeminiParsedResponse response = geminiService.generateReply(soulmate, Collections.emptyList(), "hi", false, false);

        assertThat(response.aiMessage()).isEqualTo("그냥 평범한 텍스트로 응답함");
        assertThat(response.affectionDelta()).isEqualTo(0);
    }

    @Test
    @DisplayName("시스템 프롬프트에 Soulmate 캐릭터 필드가 RCTFE 구조로 모두 치환되어 들어간다")
    void buildSystemInstruction_bindsAllSoulmateFields() {
        // Soulmate 엔티티는 @AllArgsConstructor 만 있어 positional 생성자를 사용한다.
        // 순서: (id, gender, characterImageId, characterImageUrl, name,
        //       personalityKeywords, hobbies, speechStyles, affectionScore, level, createdAt)
        Soulmate soulmate = new Soulmate(
                1L, "여성", "img", null, "유키",
                "차분함, 호기심 많음", "독서, 피아노", "존댓말, 느긋한 말투",
                0, 1, null
        );

        String rendered = ReflectionTestUtils.invokeMethod(geminiService,
                "buildSystemInstructionText", soulmate);

        // RCTFE 섹션 헤더가 모두 존재
        assertThat(rendered)
                .contains("# Role")
                .contains("# Context")
                .contains("# Task")
                .contains("# Format")
                .contains("# Example");

        // 캐릭터 필드가 슬롯에 치환되어 문자열로 등장
        assertThat(rendered)
                .contains("여성", "유키", "차분함", "독서", "존댓말");

        // 치환 후엔 플레이스홀더가 남아 있으면 안 된다
        assertThat(rendered).doesNotContain("{gender}", "{characterName}", "{personality}");
    }
}
