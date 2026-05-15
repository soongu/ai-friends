package kr.spartaclub.aifriends.voice.controller;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.common.response.ApiResponse;
import kr.spartaclub.aifriends.voice.dto.VoiceTranscriptionResponse;
import kr.spartaclub.aifriends.voice.exception.VoiceException;
import kr.spartaclub.aifriends.voice.service.VoiceTranscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

/**
 * Day 9 Step 4 — 음성 파일 업로드 → STT 변환 엔드포인트.
 *
 * <p>{@code multipart/form-data} 의 {@code audio} 파트로 받은 음성 파일을 검증하고
 * {@link VoiceTranscriptionService} 에 흘려보낸다. {@link MultipartFile#getResource()} 가
 * Spring 내장 {@code MultipartFileResource} 를 돌려주므로, Spring AI 의
 * {@code AudioTranscriptionPrompt(Resource)} 와 자연스럽게 연결된다.</p>
 *
 * <p>응답은 {@link ApiResponse} 로 래핑한다 (§4-1 게이트). 검증 실패는 {@link VoiceException}
 * 으로만 던지고 {@code GlobalExceptionHandler} 가 표준 에러 응답으로 변환한다.</p>
 *
 * <p>허용 확장자는 OpenAI Audio Transcription API 의 공식 지원 목록을 그대로 따른다
 * (mp3, mp4, mpeg, mpga, m4a, wav, webm — gpt-4o-transcribe · gpt-4o-mini-transcribe · whisper-1
 * 모두 동일 엔드포인트라 같은 자루를 받는다). 학습 단순도를 위해 파일명 확장자만 보고 거른다.
 * 최대 크기는 10MB — OpenAI 의 25MB 한도보다 보수적으로 잡아 학생 실습 비용을 억제한다.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/voice")
public class VoiceTranscriptionController {

    /** OpenAI Audio Transcription API 가 공식적으로 받아들이는 오디오 확장자. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "mp3", "mp4", "mpeg", "mpga", "m4a", "wav", "webm"
    );

    /** 학습용 최대 업로드 크기 — 10MB. OpenAI 한도(25MB) 보다 보수적. */
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final VoiceTranscriptionService voiceTranscriptionService;

    public VoiceTranscriptionController(VoiceTranscriptionService voiceTranscriptionService) {
        this.voiceTranscriptionService = voiceTranscriptionService;
    }

    @PostMapping("/transcribe")
    public ResponseEntity<ApiResponse<VoiceTranscriptionResponse>> transcribe(
            @RequestParam("audio") MultipartFile audio) {

        validate(audio);

        Resource resource = audio.getResource();
        String text = voiceTranscriptionService.transcribe(resource);

        log.info("[VoiceTranscription] success: filename={}, size={}, textLength={}",
                audio.getOriginalFilename(), audio.getSize(), text.length());

        return ResponseEntity.ok(ApiResponse.success(new VoiceTranscriptionResponse(text)));
    }

    /**
     * 업로드 검증.
     *
     * <ol>
     *   <li>비어 있으면 {@link ErrorCode#VOICE_AUDIO_REQUIRED}</li>
     *   <li>확장자 화이트리스트에 없으면 {@link ErrorCode#VOICE_AUDIO_FORMAT_INVALID}</li>
     *   <li>10MB 초과면 {@link ErrorCode#VOICE_AUDIO_TOO_LARGE}</li>
     * </ol>
     */
    private void validate(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new VoiceException(ErrorCode.VOICE_AUDIO_REQUIRED);
        }

        String extension = extractExtension(audio.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new VoiceException(ErrorCode.VOICE_AUDIO_FORMAT_INVALID);
        }

        if (audio.getSize() > MAX_BYTES) {
            throw new VoiceException(ErrorCode.VOICE_AUDIO_TOO_LARGE);
        }
    }

    /**
     * 파일명에서 확장자만 떼어내 소문자로 돌려준다. 점이 없거나 마지막 점 뒤가 비어 있으면 null.
     */
    private String extractExtension(String filename) {
        if (filename == null) return null;
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return null;
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
