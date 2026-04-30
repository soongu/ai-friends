package kr.spartaclub.aifriends.video.controller;

import kr.spartaclub.aifriends.common.exception.GlobalExceptionHandler;
import kr.spartaclub.aifriends.video.client.VideoPollingClient;
import kr.spartaclub.aifriends.video.dto.VideoJob;
import kr.spartaclub.aifriends.video.dto.VideoJobStatus;
import kr.spartaclub.aifriends.video.service.VideoCostCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VideoGenerationAsyncController.class)
@Import({GlobalExceptionHandler.class, VideoCostCalculator.class})
@TestPropertySource(properties = "aifriends.video.max-cost-usd-per-request=1.0")
class VideoGenerationAsyncControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean VideoPollingClient videoPollingClient;

    @Test
    @DisplayName("정상 제출(Kling 5초 480p $0.30) → 202 + ApiResponse.success + QUEUED 스냅샷")
    void should_return_202_when_valid_submit() throws Exception {
        VideoJob queued = VideoJob.queued("job-123");
        when(videoPollingClient.submit(any())).thenReturn(queued);

        mockMvc.perform(post("/api/video/generate-async")
                        .param("tier", "KLING")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"prompt": "a cat dancing", "durationSeconds": 5, "resolution": "480p"}
                            """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value("job-123"))
                .andExpect(jsonPath("$.data.status").value("QUEUED"));
    }

    @Test
    @DisplayName("프롬프트 공백 → 400 + VD001")
    void should_return_400_when_prompt_blank() throws Exception {
        mockMvc.perform(post("/api/video/generate-async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"prompt": "", "durationSeconds": 5, "resolution": "480p"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VD001"));
    }

    @Test
    @DisplayName("길이 11초 → 400 + VD002")
    void should_return_400_when_duration_invalid() throws Exception {
        mockMvc.perform(post("/api/video/generate-async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"prompt": "p", "durationSeconds": 11, "resolution": "480p"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VD002"));
    }

    @Test
    @DisplayName("Sora 5초 720p ($10) → max=$1 한도 초과 → 429 + VD005")
    void should_return_429_when_cost_exceeds_limit() throws Exception {
        mockMvc.perform(post("/api/video/generate-async")
                        .param("tier", "SORA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"prompt": "epic", "durationSeconds": 5, "resolution": "720p"}
                            """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("VD005"));
    }

    @Test
    @DisplayName("status/{jobId} → 200 + ApiResponse.success + SUCCEEDED + videoUrl")
    void should_return_200_when_status_succeeded() throws Exception {
        VideoJob succeeded = new VideoJob(
                "job-456", VideoJobStatus.SUCCEEDED,
                "https://stub.local/videos/job-456.mp4", null);
        when(videoPollingClient.pollStatus("job-456")).thenReturn(succeeded);

        mockMvc.perform(get("/api/video/status/job-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.videoUrl").value("https://stub.local/videos/job-456.mp4"));
    }
}
