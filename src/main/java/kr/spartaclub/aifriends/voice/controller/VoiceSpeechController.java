package kr.spartaclub.aifriends.voice.controller;

import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.common.response.ApiResponse;
import kr.spartaclub.aifriends.voice.dto.VoiceSpeechRequest;
import kr.spartaclub.aifriends.voice.exception.VoiceException;
import kr.spartaclub.aifriends.voice.service.VoiceSynthesisResult;
import kr.spartaclub.aifriends.voice.service.VoiceSynthesisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Day 9 Step 6 — 텍스트 → TTS → audio/mpeg 바이너리 응답 엔드포인트.
 *
 * <p>JSON 본문 {@code {"text": "..."}} 를 받아 {@link VoiceSynthesisService} 로 합성하고
 * 합성된 mp3 바이트를 그대로 응답 바디로 흘려보낸다. {@code Content-Type} 은
 * {@code audio/mpeg} 이고 응답은 byte[] — 브라우저의 {@code <audio>} 태그가 그대로 재생할 수 있다.</p>
 *
 * <p><b>§4-1 ApiResponse 게이트 예외 결정</b>: 본 컨트롤러의 정상 응답은 {@code ApiResponse<T>}
 * 로 래핑하지 않는다. 응답이 바이너리(audio/mpeg)라 JSON 래핑이 부적합하기 때문이다 — 게이트의
 * "디버그 전용 평문 출력" 예외 절의 바이너리 버전이다. 다만 <b>에러 응답은
 * {@code GlobalExceptionHandler} 가 자동으로 ApiResponse JSON 으로 래핑</b>하므로,
 * 같은 엔드포인트의 정상/에러 응답 형태가 비대칭(정상=binary, 에러=JSON)이다 — 이는 의도된 결.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/voice")
public class VoiceSpeechController {

    /** OpenAI TTS API 가 한 번의 호출로 받아들이는 텍스트 길이 권장 한도. */
    private static final int MAX_TEXT_LENGTH = 4000;

    private final VoiceSynthesisService voiceSynthesisService;

    public VoiceSpeechController(VoiceSynthesisService voiceSynthesisService) {
        this.voiceSynthesisService = voiceSynthesisService;
    }

    @PostMapping(value = "/speak", produces = "audio/mpeg")
    public ResponseEntity<byte[]> speak(@RequestBody VoiceSpeechRequest request) {
        validate(request);

        VoiceSynthesisResult result = voiceSynthesisService.synthesizeWithMeta(request.text(), request.voice());

        log.info("[VoiceSpeech] success: textLength={}, requestedVoice={}, provider={}, resolvedVoice={}, audioBytes={}",
                request.text().length(), request.voice(), result.provider(), result.voice(), result.audio().length);

        // 학습용 — DevTools Network 탭에서 헤더만 봐도 어떤 프로바이더로 swap 됐는지 즉시 확인 가능.
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header("Content-Disposition", "inline; filename=\"speech.mp3\"")
                .header("X-TTS-Provider", result.provider() == null ? "" : result.provider())
                .header("X-TTS-Voice",    result.voice()    == null ? "" : result.voice())
                .contentLength(result.audio().length)
                .body(result.audio());
    }

    /**
     * 활성 TTS 프로바이더 / 매핑 테이블 메타정보 조회.
     *
     * <p>DevTools 헤더만으로 부족하고 *현재 어떤 프로바이더가 살아 있는지* 단독 확인하고
     * 싶을 때 호출. 응답: {@code { provider: "elevenlabs" }}. 학습 시연용 — 학생이
     * {@code .env} 의 {@code TTS_PROVIDER} 를 바꾸고 재기동한 뒤 이 엔드포인트를 한 번
     * 찍어서 swap 이 반영됐는지 즉시 확인할 수 있다.</p>
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, String>>> info() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("provider", voiceSynthesisService.getActiveProvider())
        ));
    }

    /**
     * 요청 검증.
     *
     * <ol>
     *   <li>request 또는 text 가 null/공백이면 {@link ErrorCode#VOICE_TEXT_REQUIRED}</li>
     *   <li>4000자 초과면 {@link ErrorCode#VOICE_TEXT_TOO_LONG}</li>
     * </ol>
     */
    private void validate(VoiceSpeechRequest request) {
        if (request == null || request.text() == null || request.text().isBlank()) {
            throw new VoiceException(ErrorCode.VOICE_TEXT_REQUIRED);
        }
        if (request.text().length() > MAX_TEXT_LENGTH) {
            throw new VoiceException(ErrorCode.VOICE_TEXT_TOO_LONG);
        }
    }
}
