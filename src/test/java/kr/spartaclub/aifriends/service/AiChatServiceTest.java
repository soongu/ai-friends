package kr.spartaclub.aifriends.service;

import kr.spartaclub.aifriends.chat.dto.AiReply;
import kr.spartaclub.aifriends.chat.service.SoulmateChatService;
import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.ChatLog;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.dto.AiChatRequest;
import kr.spartaclub.aifriends.dto.AiChatResponse;
import kr.spartaclub.aifriends.repository.ChatLogRepository;
import kr.spartaclub.aifriends.repository.SoulmateAchievementRepository;
import kr.spartaclub.aifriends.repository.SoulmateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * Day 5 Step 6 — LLM 호출 위임이 {@link SoulmateChatService} 로 갈아끼워졌으므로
 * {@code GeminiService} mock 을 {@code SoulmateChatService} mock 으로 교체하고,
 * 호감도 미제공·연속 선택지 차단 보정 카운터 검증 케이스를 들어냈다.
 * 검증 초점은 *Soulmate 호감도/레벨/뱃지 갱신 + ChatLog 저장* 비즈니스 흐름.
 */
@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private SoulmateRepository soulmateRepository;

    @Mock
    private ChatLogRepository chatLogRepository;

    @Mock
    private SoulmateAchievementRepository achievementRepository;

    @Mock
    private SoulmateChatService soulmateChatService;

    @InjectMocks
    private AiChatService aiChatService;

    @Test
    @DisplayName("정상적인 채팅 프로세스 - 호감도 증가 및 뱃지 획득 없음")
    void processChat_success_noBadge() {
        // given
        AiChatRequest request = new AiChatRequest(1L, "안녕");
        Soulmate soulmate = new Soulmate(1L, "MALE", "img", null, "Bob", "keyword", "hobby", "style", 0, 1, null);

        given(soulmateRepository.findById(1L)).willReturn(Optional.of(soulmate));

        AiReply reply = new AiReply("반가워!", List.of("응", "아니"), 5);
        given(soulmateChatService.chat(eq(1L), eq("안녕"))).willReturn(reply);

        // when
        AiChatResponse response = aiChatService.processChat(request);

        // then
        assertThat(response.aiMessage()).isEqualTo("반가워!");
        assertThat(response.choices()).containsExactly("응", "아니");
        assertThat(response.affectionScore()).isEqualTo(5);
        assertThat(response.level()).isEqualTo(1);
        assertThat(response.newBadges()).isEmpty();

        // verify DB saves: user log + ai log
        then(chatLogRepository).should(times(2)).save(any(ChatLog.class));
    }

    @Test
    @DisplayName("채팅시 호감도가 임계점(10) 돌파하면 뱃지를 획득한다")
    void processChat_success_withBadge() {
        // given
        AiChatRequest request = new AiChatRequest(1L, "선물이야");
        Soulmate soulmate = new Soulmate(1L, "MALE", "img", null, "Bob", "keyword", "hobby", "style", 8, 1, null);

        given(soulmateRepository.findById(1L)).willReturn(Optional.of(soulmate));

        // affection adds 4 -> total 12 (> 10)
        AiReply reply = new AiReply("고마워!", Collections.emptyList(), 4);
        given(soulmateChatService.chat(eq(1L), eq("선물이야"))).willReturn(reply);

        given(achievementRepository.existsBySoulmateIdAndBadgeCode(1L, "AFFECTION_10")).willReturn(false);

        // when
        AiChatResponse response = aiChatService.processChat(request);

        // then
        assertThat(response.affectionScore()).isEqualTo(12);
        assertThat(response.level()).isEqualTo(2); // 1 + 12/10
        assertThat(response.newBadges()).containsExactly("AFFECTION_10");
        then(achievementRepository).should().save(any());
    }

    @Test
    @DisplayName("이성친구가 존재하지 않으면 예외가 발생한다")
    void processChat_notFound() {
        // given
        AiChatRequest request = new AiChatRequest(999L, "안녕");
        given(soulmateRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> aiChatService.processChat(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SOULMATE_NOT_FOUND.getMessage());
    }
}
