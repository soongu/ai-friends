package kr.spartaclub.aifriends.service;

import kr.spartaclub.aifriends.chat.dto.AiReply;
import kr.spartaclub.aifriends.chat.dto.SelcaResult;
import kr.spartaclub.aifriends.chat.dto.VisionCommentResult;
import kr.spartaclub.aifriends.chat.service.SelcaService;
import kr.spartaclub.aifriends.chat.service.SoulmateChatService;
import kr.spartaclub.aifriends.chat.service.VisionCommentService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Day 5 Step 6 — LLM 호출 위임이 {@link SoulmateChatService} 로 갈아끼워졌으므로
 * {@code GeminiService} mock 을 {@code SoulmateChatService} mock 으로 교체하고,
 * 호감도 미제공·연속 선택지 차단 보정 카운터 검증 케이스를 들어냈다.
 * 검증 초점은 *Soulmate 호감도/레벨/뱃지 갱신 + ChatLog 저장* 비즈니스 흐름.
 *
 * <p>Day 7 Step 9 — 셀카 요청 분기 케이스 추가. SelcaService mock 을 함께 주입하고
 * isSelcaRequest = true / false 두 분기와 가드 한도 초과 우회 응답을 검증한다.</p>
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

    @Mock
    private SelcaService selcaService;

    @Mock
    private VisionCommentService visionCommentService;

    @InjectMocks
    private AiChatService aiChatService;

    @Test
    @DisplayName("정상적인 채팅 프로세스 - 호감도 증가 및 뱃지 획득 없음 (셀카 미요청)")
    void processChat_success_noBadge() {
        // given
        AiChatRequest request = new AiChatRequest(1L, "안녕");
        Soulmate soulmate = new Soulmate(1L, "MALE", "img", null, "Bob", "keyword", "hobby", "style", 0, 1, null, null);

        given(soulmateRepository.findById(1L)).willReturn(Optional.of(soulmate));

        AiReply reply = new AiReply("반가워!", List.of("응", "아니"), 5);
        given(soulmateChatService.chat(eq(1L), eq("안녕"))).willReturn(reply);
        given(selcaService.isSelcaRequest("안녕")).willReturn(false);

        // when
        AiChatResponse response = aiChatService.processChat(request);

        // then
        assertThat(response.aiMessage()).isEqualTo("반가워!");
        assertThat(response.choices()).containsExactly("응", "아니");
        assertThat(response.affectionScore()).isEqualTo(5);
        assertThat(response.level()).isEqualTo(1);
        assertThat(response.newBadges()).isEmpty();
        assertThat(response.imageUrl()).isNull();

        then(chatLogRepository).should(times(2)).save(any(ChatLog.class));
        then(selcaService).should(never()).generate(any(), anyString());
    }

    @Test
    @DisplayName("채팅시 호감도가 임계점(10) 돌파하면 뱃지를 획득한다")
    void processChat_success_withBadge() {
        // given
        AiChatRequest request = new AiChatRequest(1L, "선물이야");
        Soulmate soulmate = new Soulmate(1L, "MALE", "img", null, "Bob", "keyword", "hobby", "style", 8, 1, null, null);

        given(soulmateRepository.findById(1L)).willReturn(Optional.of(soulmate));

        // affection adds 4 -> total 12 (> 10)
        AiReply reply = new AiReply("고마워!", Collections.emptyList(), 4);
        given(soulmateChatService.chat(eq(1L), eq("선물이야"))).willReturn(reply);
        given(selcaService.isSelcaRequest("선물이야")).willReturn(false);

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

    @Test
    @DisplayName("셀카 요청 — SelcaService 호출 + imageUrl 응답에 박힘 + LLM 응답은 그대로 흘림")
    void processChat_selca_success() {
        // given
        AiChatRequest request = new AiChatRequest(1L, "오늘 카페에서 책 읽는 셀카 보내줘");
        Soulmate soulmate = new Soulmate(
                1L, "FEMALE", "female-warm", "url", "Mia",
                "kind", "reading", "gentle", 5, 1, null,
                "20대 후반 한국인 여성, 긴 갈색 머리, 따뜻한 미소"
        );
        given(soulmateRepository.findById(1L)).willReturn(Optional.of(soulmate));

        AiReply reply = new AiReply("응 카페에서 막 책 읽고 있었어 ☕", List.of("귀엽다", "잘 어울려"), 2);
        given(soulmateChatService.chat(eq(1L), anyString())).willReturn(reply);

        given(selcaService.isSelcaRequest(anyString())).willReturn(true);
        given(selcaService.generate(eq(soulmate), anyString()))
                .willReturn(SelcaResult.success("/uploads/portraits/selca-abc.jpg"));

        // when
        AiChatResponse response = aiChatService.processChat(request);

        // then
        assertThat(response.aiMessage()).isEqualTo("응 카페에서 막 책 읽고 있었어 ☕");
        assertThat(response.imageUrl()).isEqualTo("/uploads/portraits/selca-abc.jpg");
        assertThat(response.affectionScore()).isEqualTo(7);
    }

    @Test
    @DisplayName("셀카 요청 — 한도 초과 시 LLM aiMessage 가 캐릭터 인격 우회 메시지로 덮어씌워지고 imageUrl 은 null")
    void processChat_selca_quotaExceeded_fallsBackToCharacterVoice() {
        // given
        AiChatRequest request = new AiChatRequest(1L, "셀카 보내줘");
        Soulmate soulmate = new Soulmate(
                1L, "FEMALE", "female-bright", "url", "Riko",
                "kind", "reading", "gentle", 5, 1, null,
                "20대 초반 한국인 여성, 단발 머리, 환한 미소"
        );
        given(soulmateRepository.findById(1L)).willReturn(Optional.of(soulmate));

        AiReply reply = new AiReply("응 한 장 찍어줄게!", List.of("기대돼"), 1);
        given(soulmateChatService.chat(eq(1L), eq("셀카 보내줘"))).willReturn(reply);

        given(selcaService.isSelcaRequest("셀카 보내줘")).willReturn(true);
        String fallback = "오늘 셀카 너무 많이 찍었나봐 ㅠㅠ 카메라 배터리 다 됐어... 내일 또 찍어줄게!";
        given(selcaService.generate(eq(soulmate), eq("셀카 보내줘")))
                .willReturn(SelcaResult.quotaExceeded(fallback));

        // when
        AiChatResponse response = aiChatService.processChat(request);

        // then
        assertThat(response.aiMessage()).isEqualTo(fallback);  // LLM 응답이 우회 메시지로 덮어씌워짐
        assertThat(response.imageUrl()).isNull();
        assertThat(response.choices()).containsExactly("기대돼");  // choices · affectionDelta 는 살림
        assertThat(response.affectionScore()).isEqualTo(6);
    }

    @Test
    @DisplayName("Vision 분기 — 사용자 imageUrl 첨부 시 LLM chat 호출 우회, VisionCommentService 가 응답 본문 책임 + imageUrl echo + 호감도 +1")
    void processChat_vision_success() {
        // given
        AiChatRequest request = new AiChatRequest(1L, "이거 봐, 우리 강아지!", "/uploads/portraits/upload-1.png");
        Soulmate soulmate = new Soulmate(
                1L, "MALE", "male-cheerful", "url", "현우",
                "다정함, 유머러스", "카페 투어, 야구", "친근한 반말", 0, 1, null,
                "20대 후반 한국인 남성, 짧은 검은 머리, 환한 미소"
        );
        given(soulmateRepository.findById(1L)).willReturn(Optional.of(soulmate));

        given(visionCommentService.hasImage("/uploads/portraits/upload-1.png")).willReturn(true);
        given(visionCommentService.comment(eq(soulmate), eq("/uploads/portraits/upload-1.png"), eq("이거 봐, 우리 강아지!")))
                .willReturn(VisionCommentResult.success("귀여운 강아지 사진이네! 보고만 있어도 기분이 좋아져."));

        // when
        AiChatResponse response = aiChatService.processChat(request);

        // then
        assertThat(response.aiMessage()).isEqualTo("귀여운 강아지 사진이네! 보고만 있어도 기분이 좋아져.");
        assertThat(response.imageUrl()).isEqualTo("/uploads/portraits/upload-1.png");  // 사용자 업로드 URL 그대로 echo
        assertThat(response.choices()).isEmpty();
        assertThat(response.affectionScore()).isEqualTo(1);  // 사진 보내준 친밀감 +1
        then(soulmateChatService).should(never()).chat(any(), anyString());  // LLM chat 호출 우회
        then(selcaService).should(never()).isSelcaRequest(anyString());
    }

    @Test
    @DisplayName("Vision 분기 — 한도 초과 시 캐릭터 인격 우회 메시지 + 호감도 변화 없음 + imageUrl 은 사용자 업로드 그대로 echo")
    void processChat_vision_quotaExceeded_fallsBackToCharacterVoice() {
        // given
        AiChatRequest request = new AiChatRequest(1L, "이 사진 어때?", "/uploads/portraits/upload-2.jpg");
        Soulmate soulmate = new Soulmate(
                1L, "MALE", "male-calm", "url", "지호",
                "차분함", "독서", "정중한 존댓말", 3, 1, null,
                "20대 후반 한국인 남성, 안경"
        );
        given(soulmateRepository.findById(1L)).willReturn(Optional.of(soulmate));

        given(visionCommentService.hasImage("/uploads/portraits/upload-2.jpg")).willReturn(true);
        String fallback = "오늘 사진 너무 많이 봤더니 눈이 좀 피곤하네 ㅠㅠ 내일 다시 보여줄래?";
        given(visionCommentService.comment(eq(soulmate), eq("/uploads/portraits/upload-2.jpg"), eq("이 사진 어때?")))
                .willReturn(VisionCommentResult.quotaExceeded(fallback));

        // when
        AiChatResponse response = aiChatService.processChat(request);

        // then
        assertThat(response.aiMessage()).isEqualTo(fallback);
        assertThat(response.imageUrl()).isEqualTo("/uploads/portraits/upload-2.jpg");  // 사용자 업로드는 그대로 echo
        assertThat(response.affectionScore()).isEqualTo(3);  // 한도 초과 시 호감도 변화 없음
        then(soulmateChatService).should(never()).chat(any(), anyString());
    }
}
