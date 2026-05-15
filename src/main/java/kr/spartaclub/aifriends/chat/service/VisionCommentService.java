package kr.spartaclub.aifriends.chat.service;

import kr.spartaclub.aifriends.chat.dto.VisionCommentResult;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.vision.exception.VisionException;
import kr.spartaclub.aifriends.vision.service.VisionChatService;
import kr.spartaclub.aifriends.vision.service.VisionDailyQuotaGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Day 8 후속 retrofit — 사용자 업로드 이미지 → 캐릭터 코멘트 결합 서비스.
 *
 * <p>Day 7 {@link SelcaService} 와 *대칭으로* 박았다. 셀카는 *LLM 텍스트 + AI 가 만든 이미지* 의 두 출력을
 * 합치는 결, 비전은 *LLM 의 vision 모드 응답 한 줄* 로 채팅 본문을 *대체* 하는 결 — 입력 모달리티가
 * 바뀌면 출력 합성 결도 달라지는 자리.</p>
 *
 * <p>{@code AiChatService} 는 facade 로 남고, *chat ↔ vision 결합* (가드 1회 + describe 1회 + 인격 톤
 * 후처리) 이라는 교차 관심사는 이 자리에 캡슐화한다 — Day 7 {@code SelcaService} 가 *chat ↔ image* 를
 * 캡슐화한 것과 같은 패턴.</p>
 *
 * <p><strong>한도 초과 우회</strong>: {@link VisionDailyQuotaGuard#checkAndIncrement()} 가
 * {@link VisionException}({@link ErrorCode#VISION_QUOTA_EXCEEDED}) 를 throw 하면 이 서비스가
 * *캐릭터 인격 톤* 의 우회 메시지로 변환한다. 비용 가드의 *기술적 차단* 이 *캐릭터 인격* 으로 변환되는
 * 미연시 몰입감 자리 — Day 7 {@code SelcaService} 의 정합 패턴 그대로.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisionCommentService {

    private static final String QUOTA_EXCEEDED_FALLBACK =
            "오늘 사진 너무 많이 봤더니 눈이 좀 피곤하네 ㅠㅠ 내일 다시 보여줄래?";

    private static final String VISION_FAILED_FALLBACK =
            "어, 사진이 잘 안 보이네 ㅠㅠ 다른 사진으로 한 번 더 보내줄래?";

    private final VisionChatService visionChatService;
    private final VisionDailyQuotaGuard quotaGuard;

    /**
     * 사용자가 보낸 imageUrl 이 *실제 분석 대상* 인지 판정. null/blank 이면 일반 텍스트 채팅.
     */
    public boolean hasImage(String imageUrl) {
        return imageUrl != null && !imageUrl.isBlank();
    }

    /**
     * Vision 모드로 캐릭터 코멘트 생성. 가드 1회 + describe 1회.
     *
     * <p>가드 한도 초과는 {@link VisionCommentResult#quotaExceeded(String)} 으로,
     * describe 빈 응답/예외는 {@link VisionCommentResult#failed(String)} 로 흘려보낸다.
     * 모든 실패 경로는 *캐릭터 인격 톤* 우회 메시지로 응답을 보존 — 사용자 화면에는 *기술 디테일이 새지 않게*.</p>
     */
    public VisionCommentResult comment(Soulmate soulmate, String imageUrl, String userMessage) {
        try {
            quotaGuard.checkAndIncrement();
        } catch (VisionException e) {
            if (e.getErrorCode() == ErrorCode.VISION_QUOTA_EXCEEDED) {
                log.info("[VisionComment] quota exceeded — falling back to character voice");
                return VisionCommentResult.quotaExceeded(QUOTA_EXCEEDED_FALLBACK);
            }
            throw e;
        }

        try {
            String prompt = composePrompt(soulmate, userMessage);
            String aiComment = visionChatService.describe(imageUrl, prompt);
            if (aiComment == null || aiComment.isBlank()) {
                log.warn("[VisionComment] describe returned blank — falling back");
                return VisionCommentResult.failed(VISION_FAILED_FALLBACK);
            }
            log.info("[VisionComment] success: soulmateId={}, imageUrl={}", soulmate.getId(), imageUrl);
            return VisionCommentResult.success(aiComment);
        } catch (RuntimeException e) {
            log.warn("[VisionComment] vision call failed: {}", e.getMessage());
            return VisionCommentResult.failed(VISION_FAILED_FALLBACK);
        }
    }

    /**
     * 캐릭터 컨텍스트(이름 · 성격 · 취미) + 사용자 자유 입력을 한 덩어리 prompt 로 합성한다.
     * 학습용 단순도 유지 — SystemMessage 분리 없이 자연어 한 덩어리.
     */
    String composePrompt(Soulmate soulmate, String userMessage) {
        String trimmed = userMessage == null ? "" : userMessage.trim();
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 '").append(soulmate.getName()).append("' 라는 이름의 캐릭터예요. ")
                .append("성격: ").append(soulmate.getPersonalityKeywords()).append(". ")
                .append("취미: ").append(soulmate.getHobbies()).append(". ")
                .append("사용자가 사진을 보내왔어요. ");
        if (!trimmed.isEmpty()) {
            sb.append("사용자 메시지: \"").append(trimmed).append("\". ");
        }
        sb.append("이 사진을 보고 한국어 한~두 문장으로 친근한 말투로 코멘트해 주세요.");
        return sb.toString();
    }
}
