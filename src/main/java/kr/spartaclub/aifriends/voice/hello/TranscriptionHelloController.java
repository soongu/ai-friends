package kr.spartaclub.aifriends.voice.hello;

import java.io.IOException;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Day 9 보조 sandbox — {@link TranscriptionModel} 을 가장 짧게 부르는 hello world 스니펫.
 *
 * <p>업로드된 음성 파일 한 개를 그대로 STT 모델에 넘기고, 인식된 텍스트를 plain text 로
 * 돌려준다. 본 게임 코드({@code VoiceTranscriptionService}) 는 검증 · 예외 변환 · 로깅을
 * 덧붙이지만, "자매 빈을 부르는 핵심 한 줄" 은 이 컨트롤러로 환원된다. Day 9 본 교안의
 * Step 3 이후 코드와 짝패로 두고, 학생이 *모델 호출 자체의 최소 형태* 를 손에 잡아보는
 * 학습용 sandbox 다. 본 교안 본문엔 등장하지 않으며, IDE 에서 직접 펼쳐 시연할 때만 쓴다.</p>
 *
 * <p>활성화 조건: {@code spring.ai.model.audio.transcription=openai} 프로파일 + OPENAI_API_KEY
 * 가 .env 에 박혀 있어야 한다.</p>
 */
@RestController
@RequestMapping("/api/voice/hello")
public class TranscriptionHelloController {

    private final TranscriptionModel transcriptionModel;

    public TranscriptionHelloController(TranscriptionModel transcriptionModel) {
        this.transcriptionModel = transcriptionModel;
    }

    @PostMapping(value = "/transcribe", produces = "text/plain;charset=UTF-8")
    public String transcribe(@RequestParam("audio") MultipartFile audio) throws IOException {
        ByteArrayResource resource = new ByteArrayResource(audio.getBytes()) {
            @Override
            public String getFilename() {
                return audio.getOriginalFilename();
            }
        };
        return transcriptionModel.call(new AudioTranscriptionPrompt(resource))
                .getResult()
                .getOutput();
    }
}
