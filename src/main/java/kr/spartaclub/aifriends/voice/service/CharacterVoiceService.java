package kr.spartaclub.aifriends.voice.service;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.repository.SoulmateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Day 9 Step 7 — 음성 대화 5단 파이프라인.
 *
 * <p>사용자 마이크 음성({@link Resource}) 한 개를 받아 다음 다섯 단계를 거쳐 캐릭터의 음성
 * 응답({@code byte[]}) 을 돌려준다.</p>
 *
 * <ol>
 *   <li>STT — {@link VoiceTranscriptionService#transcribe(Resource)} 로 음성 → 텍스트</li>
 *   <li>캐릭터 페르소나(이름·성격·말투) + 사용자 텍스트로 {@link Prompt} 조립</li>
 *   <li>{@link ChatModel#call(Prompt)} 으로 캐릭터 응답 텍스트 생성</li>
 *   <li>TTS — {@link VoiceSynthesisService#synthesize(String)} 로 텍스트 → 음성</li>
 *   <li>합성된 byte[] 반환</li>
 * </ol>
 *
 * <p>Day 8 의 {@code CharacterVisionService} 와 같은 결 — 캐릭터 도메인(Soulmate) 과 모달리티
 * 서비스(Voice) 를 결합한 *진입점* 이다. {@link ChatModel} 은 인터페이스로만 주입받아
 * 프로바이더 추상화 게이트(§4) 를 지킨다.</p>
 *
 * <p><b>ChatMemory 미적용 결정</b>: 학습용 단순도를 위해 이번 Step 에서는 {@link ChatModel}
 * 직접 호출만 한다. Day 5 에서 박은 {@code JdbcChatMemoryRepository} 를 음성 대화 파이프라인에
 * 붙이는 회수는 Day 9 마무리 또는 과제 자리에서 다룬다.</p>
 *
 * <p><b>예외 정책</b>:
 * <ul>
 *   <li>캐릭터 없음 → {@link BusinessException}({@link ErrorCode#SOULMATE_NOT_FOUND})</li>
 *   <li>STT 결과가 빈 문자열이면 ChatModel/TTS 를 건너뛰고 빈 byte[] 반환 (학습 단순도 — 무성/노이즈 입력 폴백)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
public class CharacterVoiceService {

    private final SoulmateRepository soulmateRepository;
    private final VoiceTranscriptionService voiceTranscriptionService;
    private final VoiceSynthesisService voiceSynthesisService;
    private final ChatModel chatModel;

    public CharacterVoiceService(SoulmateRepository soulmateRepository,
                                 VoiceTranscriptionService voiceTranscriptionService,
                                 VoiceSynthesisService voiceSynthesisService,
                                 ChatModel chatModel) {
        this.soulmateRepository = soulmateRepository;
        this.voiceTranscriptionService = voiceTranscriptionService;
        this.voiceSynthesisService = voiceSynthesisService;
        this.chatModel = chatModel;
    }

    /**
     * 음성 대화 5단 파이프라인을 실행한다.
     *
     * @param soulmateId 대화 상대 캐릭터 PK
     * @param audio      사용자 마이크 음성 리소스
     * @return 캐릭터 음성 응답 byte[] (STT 결과가 빈 문자열이면 빈 byte[])
     */
    @Transactional(readOnly = true)
    public byte[] converse(Long soulmateId, Resource audio) {
        Soulmate soulmate = soulmateRepository.findById(soulmateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOULMATE_NOT_FOUND));

        // 1) STT
        String userText = voiceTranscriptionService.transcribe(audio);
        if (userText == null || userText.isBlank()) {
            log.info("[CharacterVoice] STT empty — skip Chat/TTS, soulmateId={}", soulmateId);
            return new byte[0];
        }

        // 2~3) 페르소나 + 사용자 텍스트 → ChatModel
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(buildPersonaSystemMessage(soulmate)),
                new UserMessage(userText)
        ));
        ChatResponse response = chatModel.call(prompt);

        String aiText = (response == null || response.getResult() == null
                || response.getResult().getOutput() == null
                || response.getResult().getOutput().getText() == null)
                ? ""
                : response.getResult().getOutput().getText();

        if (aiText.isBlank()) {
            log.info("[CharacterVoice] ChatModel empty response — skip TTS, soulmateId={}", soulmateId);
            return new byte[0];
        }

        log.info("[CharacterVoice] converse: soulmateId={}, name={}, userTextLen={}, aiTextLen={}",
                soulmate.getId(), soulmate.getName(), userText.length(), aiText.length());

        // 4~5) TTS
        return voiceSynthesisService.synthesize(aiText);
    }

    /**
     * 캐릭터 컨텍스트(이름 · 성격 · 취미 · 말투) 를 박은 SystemMessage.
     * 학습용 단순도 — PromptTemplate 까지 가지 않고 자연어 한 덩어리로 둔다.
     */
    private String buildPersonaSystemMessage(Soulmate soulmate) {
        return "당신은 '" + soulmate.getName() + "' 라는 이름의 캐릭터예요. "
                + "성격: " + soulmate.getPersonalityKeywords() + ". "
                + "취미: " + soulmate.getHobbies() + ". "
                + "말투: " + soulmate.getSpeechStyles() + ". "
                + "사용자의 말에 한국어 1~2 문장으로, 캐릭터 말투를 유지하며 짧게 답해 주세요.";
    }
}
