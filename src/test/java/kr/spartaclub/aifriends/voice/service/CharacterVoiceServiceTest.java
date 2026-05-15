package kr.spartaclub.aifriends.voice.service;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.repository.SoulmateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Day 9 Step 7 — CharacterVoiceService 단위 테스트.
 *
 * <p>5단 파이프라인(STT → ChatModel → TTS) 의 각 단계가 순서대로 호출되고, ChatModel 에 들어간
 * Prompt 안에 캐릭터 페르소나(이름·성격) + STT 결과 텍스트가 같이 박혀 있는지 검증한다.
 * 외부 프로바이더 호출은 전부 모킹.</p>
 */
@ExtendWith(MockitoExtension.class)
class CharacterVoiceServiceTest {

    @Mock
    SoulmateRepository soulmateRepository;

    @Mock
    VoiceTranscriptionService voiceTranscriptionService;

    @Mock
    VoiceSynthesisService voiceSynthesisService;

    @Mock
    ChatModel chatModel;

    @Mock
    Resource audio;

    @InjectMocks
    CharacterVoiceService characterVoiceService;

    @Test
    @DisplayName("정상 흐름: STT → ChatModel(페르소나+사용자텍스트) → TTS 가 순서대로 호출되고 합성된 byte[] 가 반환된다")
    void should_run_full_pipeline_in_order() {
        // given
        Soulmate soulmate = new Soulmate(
                1L, "FEMALE", "img1", "https://image/portrait.png", "Alice",
                "차분함, 다정함", "독서, 산책", "존댓말",
                0, 1, LocalDateTime.now(), null);
        when(soulmateRepository.findById(1L)).thenReturn(Optional.of(soulmate));
        when(voiceTranscriptionService.transcribe(audio)).thenReturn("오늘 뭐했어?");

        ChatResponse chatResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage("오늘은 책을 읽었어요."), ChatGenerationMetadata.NULL)
        ));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        byte[] stubAudio = new byte[]{0x49, 0x44, 0x33, 0x04};
        when(voiceSynthesisService.synthesize("오늘은 책을 읽었어요.")).thenReturn(stubAudio);

        // when
        byte[] result = characterVoiceService.converse(1L, audio);

        // then — 결과
        assertThat(result).isEqualTo(stubAudio);

        // then — ChatModel 에 들어간 Prompt 검증
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        Prompt captured = promptCaptor.getValue();

        // Prompt 의 메시지들을 합쳐서 페르소나 + 사용자 텍스트가 모두 들어있는지 확인
        String allText = captured.getInstructions().stream()
                .map(Message::getText)
                .reduce("", (a, b) -> a + "\n" + b);
        assertThat(allText).contains("Alice");
        assertThat(allText).contains("차분함");
        assertThat(allText).contains("오늘 뭐했어?");

        // UserMessage 가 마지막에 박혀 있는지 (캐릭터 컨텍스트는 SystemMessage, 사용자 발화는 UserMessage)
        List<Message> messages = captured.getInstructions();
        assertThat(messages.get(messages.size() - 1)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(messages.size() - 1).getText()).contains("오늘 뭐했어?");
    }

    @Test
    @DisplayName("캐릭터를 찾을 수 없으면 BusinessException(SOULMATE_NOT_FOUND), STT/ChatModel/TTS 모두 호출 안 됨")
    void should_throw_when_soulmate_not_found() {
        when(soulmateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> characterVoiceService.converse(99L, audio))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SOULMATE_NOT_FOUND);

        verifyNoInteractions(voiceTranscriptionService);
        verifyNoInteractions(chatModel);
        verifyNoInteractions(voiceSynthesisService);
    }

    @Test
    @DisplayName("STT 가 빈 문자열을 돌려주면 ChatModel/TTS 는 호출되지 않고 빈 byte[] 가 반환된다")
    void should_short_circuit_when_stt_returns_blank() {
        Soulmate soulmate = new Soulmate(
                1L, "FEMALE", "img1", "https://image/portrait.png", "Alice",
                "차분함", "독서", "존댓말",
                0, 1, LocalDateTime.now(), null);
        when(soulmateRepository.findById(1L)).thenReturn(Optional.of(soulmate));
        when(voiceTranscriptionService.transcribe(audio)).thenReturn("");

        byte[] result = characterVoiceService.converse(1L, audio);

        assertThat(result).isEmpty();
        verify(chatModel, never()).call(any(Prompt.class));
        verify(voiceSynthesisService, never()).synthesize(anyString());
    }
}
