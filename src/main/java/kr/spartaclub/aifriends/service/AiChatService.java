package kr.spartaclub.aifriends.service;

import kr.spartaclub.aifriends.chat.dto.AiReply;
import kr.spartaclub.aifriends.chat.dto.SelcaResult;
import kr.spartaclub.aifriends.chat.service.SelcaService;
import kr.spartaclub.aifriends.chat.service.SoulmateChatService;
import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.ChatLog;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.domain.SoulmateAchievement;
import kr.spartaclub.aifriends.dto.AiChatRequest;
import kr.spartaclub.aifriends.dto.AiChatResponse;
import kr.spartaclub.aifriends.repository.ChatLogRepository;
import kr.spartaclub.aifriends.repository.SoulmateAchievementRepository;
import kr.spartaclub.aifriends.repository.SoulmateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 실시간 AI 채팅 비즈니스 흐름을 관장하는 퍼사드(Facade) 서비스.
 *
 * <p>Day 5 Step 6 — LLM 호출 부분을 {@link SoulmateChatService#chat(Long, String)} 위임으로 갈아끼웠다.
 * 기존 {@code GeminiService} (RestClient + 수동 JSON Schema 조립 + ObjectMapper 파싱) 30 줄,
 * 그리고 호감도 미제공·연속 선택지 차단 보정 카운터 30 줄을 들어냈다.
 * ChatMemory 가 멀티턴 컨텍스트를 자동 주입하므로 ChatLog 의 최근 N 건을 다시 끌어와
 * 컨텍스트로 합치는 코드도 사라졌다 (ChatLog 는 *사람이 보는 비즈니스 로그* 로 역할 분리).</p>
 *
 * <p>Day 7 Step 9 — 셀카 요청 분기 추가. {@link SelcaService#isSelcaRequest(String)} 가 true 면
 * LLM 응답과 *별도로* {@link SelcaService#generate(Soulmate, String)} 가 1회 호출되어 캐릭터의
 * 외모 일관성이 유지된 셀카 이미지를 생성한다. 가드 한도 초과 시에는 LLM 응답의 {@code aiMessage} 를
 * 캐릭터 인격 톤의 우회 메시지로 덮어쓴다 (몰입감 유지).</p>
 */
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final SoulmateRepository soulmateRepository;
    private final ChatLogRepository chatLogRepository;
    private final SoulmateAchievementRepository achievementRepository;

    private final SoulmateChatService soulmateChatService;
    private final SelcaService selcaService;

    /**
     * 사용자의 입력 메시지를 받아 AI 의 응답을 생성하고 결과를 반환한다.
     *
     * <p>Day 7 Step 9 — 셀카 요청 분기:
     * <ol>
     *   <li>{@link SelcaService#isSelcaRequest} 가 true 면 LLM 호출 후 추가로 이미지 1장 생성.</li>
     *   <li>이미지 생성 한도 초과 시 LLM 응답의 {@code aiMessage} 를 캐릭터 인격 우회 메시지로 덮어쓰기.</li>
     *   <li>이미지 생성 실패 시에도 동일 — 챗 응답은 살리되 imageUrl 만 null 로 흘려보냄.</li>
     * </ol>
     * 호감도/뱃지/ChatLog 처리는 셀카 응답에도 동일하게 적용된다.</p>
     */
    @Transactional
    public AiChatResponse processChat(AiChatRequest request) {
        Long soulmateId = request.soulmateId();
        String userMessage = request.userMessage();

        // 1. 대화 상대(이성친구) 조회 — 호감도/레벨 갱신 대상
        Soulmate soulmate = soulmateRepository.findById(soulmateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOULMATE_NOT_FOUND));

        // 2. LLM 호출 — Spring AI ChatClient + ChatMemory + system-v1.st 페르소나
        AiReply reply = soulmateChatService.chat(soulmateId, userMessage);

        // 2-1. (Day 7 Step 9) 셀카 요청 분기 — 키워드 매칭 시 이미지 1장 추가 생성
        String imageUrl = null;
        if (selcaService.isSelcaRequest(userMessage)) {
            SelcaResult selca = selcaService.generate(soulmate, userMessage);
            if (selca.imageUrl() != null) {
                imageUrl = selca.imageUrl();
            } else {
                // 한도 초과 / 생성 실패 — LLM 응답 본문을 캐릭터 인격 우회 메시지로 덮어쓰기 (choices · affectionDelta 는 살림)
                reply = new AiReply(selca.fallbackMessage(), reply.choices(), reply.affectionDelta());
            }
        }

        // 3. 비즈니스 로그(ChatLog) 저장 — ChatMemory 와는 별개의 "사람이 보는" 채널
        chatLogRepository.save(new ChatLog(null, soulmateId, "USER", userMessage, null));
        chatLogRepository.save(new ChatLog(null, soulmateId, "AI", reply.aiMessage(), null));

        // 4. 호감도 및 레벨 갱신 (Dirty Checking)
        soulmate.addAffection(reply.affectionDelta());
        int newLevel = 1 + (soulmate.getAffectionScore() / 10);
        soulmate.setLevel(newLevel);

        // 5. 업적(뱃지) 획득 여부 체크
        List<String> newBadges = checkAndGrantBadges(soulmate);

        // 6. 클라이언트 응답 객체
        return new AiChatResponse(
                userMessage,
                reply.aiMessage(),
                reply.choices(),
                soulmate.getId(),
                soulmate.getAffectionScore(),
                soulmate.getLevel(),
                newBadges,
                imageUrl
        );
    }

    /**
     * 특정 조건 도달 시 뱃지를 부여한다.
     */
    private List<String> checkAndGrantBadges(Soulmate soulmate) {
        List<String> newBadges = new ArrayList<>();
        Long soulmateId = soulmate.getId();
        int affection = soulmate.getAffectionScore();

        if (affection >= 10 && !achievementRepository.existsBySoulmateIdAndBadgeCode(soulmateId, "AFFECTION_10")) {
            achievementRepository.save(new SoulmateAchievement(null, soulmateId, "AFFECTION_10", null));
            newBadges.add("AFFECTION_10");
        }

        if (affection >= 50 && !achievementRepository.existsBySoulmateIdAndBadgeCode(soulmateId, "AFFECTION_50")) {
            achievementRepository.save(new SoulmateAchievement(null, soulmateId, "AFFECTION_50", null));
            newBadges.add("AFFECTION_50");
        }

        return newBadges;
    }
}
