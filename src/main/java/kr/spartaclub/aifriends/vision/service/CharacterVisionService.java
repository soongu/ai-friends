package kr.spartaclub.aifriends.vision.service;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.repository.SoulmateRepository;
import kr.spartaclub.aifriends.vision.dto.SoulmateIntroductionResponse;
import kr.spartaclub.aifriends.vision.exception.VisionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Day 8 Step 6 — Day 7 portrait 회수의 클로저.
 *
 * <p>캐릭터(Soulmate) ID 를 받아 그 캐릭터의 {@code characterImageUrl} 을
 * {@link VisionChatService#describe(String, String)} 의 입력으로 그대로 흘려보내, 응답으로
 * <em>캐릭터가 자기 사진을 보고 한 자기소개</em> 텍스트를 돌려준다.</p>
 *
 * <p>학습용 단순도 — SystemMessage 분리는 하지 않는다. 캐릭터 컨텍스트(성격 · 취미)는
 * {@link #buildIntroductionPrompt(Soulmate)} 가 만든 자연어 프롬프트 한 덩어리에 박는다.</p>
 *
 * <p>예외 정책:
 * <ul>
 *   <li>캐릭터 없음 → {@link BusinessException}({@link ErrorCode#SOULMATE_NOT_FOUND})</li>
 *   <li>{@code characterImageUrl} 이 null/blank → {@link VisionException}({@link ErrorCode#VISION_PORTRAIT_NOT_AVAILABLE})</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
public class CharacterVisionService {

    private final SoulmateRepository soulmateRepository;
    private final VisionChatService visionChatService;
    private final VisionDailyQuotaGuard quotaGuard;

    public CharacterVisionService(SoulmateRepository soulmateRepository,
                                  VisionChatService visionChatService,
                                  VisionDailyQuotaGuard quotaGuard) {
        this.soulmateRepository = soulmateRepository;
        this.visionChatService = visionChatService;
        this.quotaGuard = quotaGuard;
    }

    @Transactional(readOnly = true)
    public SoulmateIntroductionResponse introduce(Long soulmateId) {
        quotaGuard.checkAndIncrement();

        Soulmate soulmate = soulmateRepository.findById(soulmateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOULMATE_NOT_FOUND));

        String portraitUrl = soulmate.getCharacterImageUrl();
        if (portraitUrl == null || portraitUrl.isBlank()) {
            throw new VisionException(ErrorCode.VISION_PORTRAIT_NOT_AVAILABLE);
        }

        String prompt = buildIntroductionPrompt(soulmate);
        String introduction = visionChatService.describe(portraitUrl, prompt);

        log.info("[CharacterVision] introduce: soulmateId={}, name={}, portraitUrl={}",
                soulmate.getId(), soulmate.getName(), portraitUrl);

        return new SoulmateIntroductionResponse(
                soulmate.getId(),
                soulmate.getName(),
                portraitUrl,
                introduction
        );
    }

    /**
     * 캐릭터 컨텍스트(이름 · 성격 키워드 · 취미)를 박은 자기소개 프롬프트.
     * 학습용 단순도 유지를 위해 SystemMessage 분리 없이 한 덩어리 텍스트로 전달한다.
     */
    private String buildIntroductionPrompt(Soulmate soulmate) {
        return "당신은 '" + soulmate.getName() + "' 라는 이름의 캐릭터예요. "
                + "성격: " + soulmate.getPersonalityKeywords() + ". "
                + "취미: " + soulmate.getHobbies() + ". "
                + "이 그림은 당신의 자화상이에요. 그림을 보고 한국어 2~3 문장으로 자기소개해 주세요. 친근한 말투로요.";
    }
}
