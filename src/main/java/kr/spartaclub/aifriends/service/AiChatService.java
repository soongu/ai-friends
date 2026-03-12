package kr.spartaclub.aifriends.service;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.ChatLog;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.domain.SoulmateAchievement;
import kr.spartaclub.aifriends.dto.AiChatRequest;
import kr.spartaclub.aifriends.dto.AiChatResponse;
import kr.spartaclub.aifriends.dto.GeminiParsedResponse;
import kr.spartaclub.aifriends.repository.ChatLogRepository;
import kr.spartaclub.aifriends.repository.SoulmateAchievementRepository;
import kr.spartaclub.aifriends.repository.SoulmateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실시간 AI 채팅 비즈니스 흐름을 관장하는 퍼사드(Facade) 역할의 서비스입니다.
 * Soulmate, ChatLog, GeminiService 여러 도메인의 로직을 하나로 엮어냅니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final SoulmateRepository soulmateRepository;
    private final ChatLogRepository chatLogRepository;
    private final SoulmateAchievementRepository achievementRepository;
    
    // 외부 API 통신 담당
    private final GeminiService geminiService;

    // AI에게 한 번에 전달할 과거 대화 개수 상한 (예: 20건 = 최근 10턴)
    private static final int RECENT_LOGS_LIMIT = 20;

    /** soulmateId별로 호감도(affectionDelta)가 연속으로 미제공된 횟수. 2회 이상이면 보정 프롬프트 전송 */
    private final Map<Long, Integer> consecutiveAffectionMissingBySoulmate = new ConcurrentHashMap<>();

    /** soulmateId별로 연속으로 선택지(choices)가 제공된 횟수. 2회 이상이면 다음 턴에는 선택지 금지 */
    private final Map<Long, Integer> consecutiveChoicesShownBySoulmate = new ConcurrentHashMap<>();

    /**
     * 사용자의 입력 메시지를 받아 AI의 응답을 생성하고 결과를 반환합니다.
     */
    @Transactional
    public AiChatResponse processChat(AiChatRequest request) {
        Long soulmateId = request.soulmateId();
        String userMessage = request.userMessage();

        // 1. 대화 상대(이성친구) 정보 조회
        Soulmate soulmate = soulmateRepository.findById(soulmateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOULMATE_NOT_FOUND));

        // 2. 과거 채팅 기록 조회 (최근 N건을 최신순으로 가져와서 과거순으로 뒤집어야 Gemini가 맥락을 이해함)
        List<ChatLog> recentLogsDesc = chatLogRepository
                .findBySoulmateIdOrderByCreatedAtDesc(soulmateId, PageRequest.of(0, RECENT_LOGS_LIMIT))
                .getContent();
        
        List<ChatLog> recentLogsAsc = new ArrayList<>(recentLogsDesc);
        Collections.reverse(recentLogsAsc); // 시간 오름차순(과거 -> 최신) 정렬

        // 3. 2회 연속 호감도 미제공 시 / 2회 연속 선택지 제공 시 보정 프롬프트와 함께 Gemini 호출
        int missingCount = consecutiveAffectionMissingBySoulmate.getOrDefault(soulmateId, 0);
        boolean requireAffectionInResponse = missingCount >= 2;
        if (requireAffectionInResponse) {
            log.info("soulmateId={} 호감도 {}회 연속 미제공 → 보정 프롬프트 추가", soulmateId, missingCount);
        }

        int consecutiveChoices = consecutiveChoicesShownBySoulmate.getOrDefault(soulmateId, 0);
        boolean forceNoChoices = consecutiveChoices >= 2;
        if (forceNoChoices) {
            log.info("soulmateId={} 선택지 {}회 연속 제공 → 이번 턴 선택지 금지", soulmateId, consecutiveChoices);
        }

        GeminiParsedResponse parsedResponse = geminiService.generateReply(soulmate, recentLogsAsc, userMessage, requireAffectionInResponse, forceNoChoices);
        String aiMessage = parsedResponse.aiMessage();
        List<String> choices = parsedResponse.choices();

        // 호감도 미제공 연속 횟수 갱신 (다음 턴에서 보정 여부 판단용)
        if (parsedResponse.affectionDelta() != 0) {
            consecutiveAffectionMissingBySoulmate.put(soulmateId, 0);
        } else {
            consecutiveAffectionMissingBySoulmate.merge(soulmateId, 1, Integer::sum);
        }

        // 선택지 연속 제공 횟수 갱신: 이번에 선택지 금지였거나 비어 있으면 0, 비어 있지 않으면 +1
        if (forceNoChoices || choices == null || choices.isEmpty()) {
            consecutiveChoicesShownBySoulmate.put(soulmateId, 0);
        } else {
            consecutiveChoicesShownBySoulmate.merge(soulmateId, 1, Integer::sum);
        }

        // 4. 대화 내용 DB 저장 (사용자 발화, AI 발화 둘 다)
        ChatLog userLog = new ChatLog(null, soulmateId, "USER", userMessage, null);
        chatLogRepository.save(userLog);
        
        ChatLog aiLog = new ChatLog(null, soulmateId, "AI", aiMessage, null);
        chatLogRepository.save(aiLog);

        // 5. 호감도 및 레벨 상승/하락 처리 (AI 평가 기반)
        int affectionDelta = parsedResponse.affectionDelta();
        soulmate.addAffection(affectionDelta);
        
        // 레벨 계산: 기본 1레벨 + (호감도 / 10). 최대 10레벨.
        int newLevel = 1 + (soulmate.getAffectionScore() / 10);
        soulmate.setLevel(newLevel);
        
        // 엔티티 값이 변경되었으므로 @Transactional 에 의해 커밋 시 자동 업데이트(Dirty Checking)

        // 6. 업적(뱃지) 획득 여부 체킹
        List<String> newBadges = checkAndGrantBadges(soulmate);

        // 7. 클라이언트로 반환할 종합 응답 객체 생성
        return new AiChatResponse(
                userMessage,
                aiMessage,
                choices,
                soulmate.getId(),
                soulmate.getAffectionScore(),
                soulmate.getLevel(),
                newBadges
        );
    }

    /**
     * 특정 조건에 도달 시 뱃지를 부여하는 로직입니다.
     */
    private List<String> checkAndGrantBadges(Soulmate soulmate) {
        List<String> newBadges = new ArrayList<>();
        Long soulmateId = soulmate.getId();
        int affection = soulmate.getAffectionScore();

        // 예시 조건 1: 호감도 10 달성 (첫 번째 데이트 제안 가능)
        if (affection >= 10 && !achievementRepository.existsBySoulmateIdAndBadgeCode(soulmateId, "AFFECTION_10")) {
            achievementRepository.save(new SoulmateAchievement(null, soulmateId, "AFFECTION_10", null));
            newBadges.add("AFFECTION_10");
        }
        
        // 예시 조건 2: 호감도 50 달성 (사랑의 시작)
        if (affection >= 50 && !achievementRepository.existsBySoulmateIdAndBadgeCode(soulmateId, "AFFECTION_50")) {
            achievementRepository.save(new SoulmateAchievement(null, soulmateId, "AFFECTION_50", null));
            newBadges.add("AFFECTION_50");
        }

        return newBadges;
    }
}
