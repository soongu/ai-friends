package kr.spartaclub.aifriends.voice.controller;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.voice.exception.VoiceException;
import kr.spartaclub.aifriends.voice.service.CharacterVoiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

/**
 * Day 9 Step 7 — 음성 대화 5단 파이프라인 진입점.
 *
 * <p>{@code multipart/form-data} 의 {@code audio} 파트로 받은 음성 파일을 검증하고
 * {@link CharacterVoiceService#converse(Long, Resource)} 에 흘려보낸 결과인 합성 음성 바이트를
 * 그대로 응답 바디로 돌려준다 (Step 6 {@code VoiceSpeechController} 와 동일한 audio/mpeg 결).</p>
 *
 * <p>입력 검증은 Step 4 {@code VoiceTranscriptionController} 와 동일한 화이트리스트 정책을
 * 그대로 답습한다 — 학습용 단순도를 위해 공통 헬퍼로 추출하지 않고 컨트롤러마다 명시적으로 둔다.</p>
 *
 * <p><b>§4-1 ApiResponse 게이트 예외 결정</b>: 정상 응답은 바이너리(audio/mpeg) 라
 * {@code ApiResponse<T>} 로 래핑하지 않는다. 에러 응답은 {@code GlobalExceptionHandler} 가
 * 자동으로 ApiResponse JSON 으로 래핑하므로, Step 6 와 동일한 비대칭(정상=binary, 에러=JSON) 결.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/voice")
public class CharacterVoiceController {

    /** OpenAI Whisper 가 공식적으로 받아들이는 오디오 확장자 (Step 4 와 동일). */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "mp3", "mp4", "mpeg", "mpga", "m4a", "wav", "webm"
    );

    /** 학습용 최대 업로드 크기 — 10MB. Whisper 한도(25MB) 보다 보수적. */
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final CharacterVoiceService characterVoiceService;

    public CharacterVoiceController(CharacterVoiceService characterVoiceService) {
        this.characterVoiceService = characterVoiceService;
    }

    @PostMapping(value = "/characters/{soulmateId}/converse", produces = "audio/mpeg")
    public ResponseEntity<byte[]> converse(
            @PathVariable Long soulmateId,
            @RequestParam("audio") MultipartFile audio) {

        validate(audio);

        Resource resource = audio.getResource();
        byte[] responseAudio = characterVoiceService.converse(soulmateId, resource);

        log.info("[CharacterVoice] success: soulmateId={}, filename={}, inSize={}, outBytes={}",
                soulmateId, audio.getOriginalFilename(), audio.getSize(), responseAudio.length);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header("Content-Disposition", "inline; filename=\"reply.mp3\"")
                .contentLength(responseAudio.length)
                .body(responseAudio);
    }

    /**
     * 업로드 검증 (Step 4 와 동일한 패턴 — 공통 헬퍼 추출 안 함).
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

    private String extractExtension(String filename) {
        if (filename == null) return null;
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return null;
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
