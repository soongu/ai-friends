package kr.spartaclub.aifriends.voice.hello;

import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 9 보조 sandbox — {@link TextToSpeechModel} 을 가장 짧게 부르는 hello world 스니펫.
 *
 * <p>쿼리 파라미터로 받은 텍스트를 그대로 TTS 모델에 넘기고, 합성된 mp3 byte[] 를
 * {@code audio/mpeg} 로 응답한다. 본 게임 코드({@code VoiceSynthesisService}) 는 길이 제한 ·
 * mood→voice 매핑 · 프로바이더 메타데이터 헤더를 덧붙이지만, "자매 빈을 부르는 핵심 한 줄"
 * 은 이 컨트롤러로 환원된다. Day 9 본 교안의 Step 5~6 이후 코드와 짝패로 두고, 학생이
 * *모델 호출 자체의 최소 형태* 를 손에 잡아보는 학습용 sandbox 다. 본 교안 본문엔 등장하지
 * 않으며, IDE 에서 직접 펼쳐 시연할 때만 쓴다.</p>
 *
 * <p>활성화 조건: {@code spring.ai.model.audio.speech=openai} (또는 {@code elevenlabs})
 * 프로파일 + 해당 프로바이더 API 키가 .env 에 박혀 있어야 한다.
 * 호출 예: {@code GET /api/voice/hello/speak?text=안녕}.</p>
 */
@RestController
@RequestMapping("/api/voice/hello")
public class SpeechHelloController {

    private final TextToSpeechModel textToSpeechModel;

    public SpeechHelloController(TextToSpeechModel textToSpeechModel) {
        this.textToSpeechModel = textToSpeechModel;
    }

    @GetMapping(value = "/speak", produces = "audio/mpeg")
    public ResponseEntity<byte[]> speak(@RequestParam("text") String text) {
        byte[] audio = textToSpeechModel.call(new TextToSpeechPrompt(text))
                .getResult()
                .getOutput();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audio);
    }
}
