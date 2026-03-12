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
}
