package kr.spartaclub.aifriends.service;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.CharacterPreset;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.domain.SoulmateAchievement;
import kr.spartaclub.aifriends.dto.SoulmateCreateRequest;
import kr.spartaclub.aifriends.dto.SoulmateProfileResponse;
import kr.spartaclub.aifriends.dto.SoulmateResponse;
import kr.spartaclub.aifriends.image.dto.ImageGenerationResult;
import kr.spartaclub.aifriends.image.service.ImageGenerationService;
import kr.spartaclub.aifriends.repository.SoulmateAchievementRepository;
import kr.spartaclub.aifriends.repository.SoulmateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 이성친구(Soulmate) 관련 비즈니스 로직을 처리하는 서비스입니다.
 * 외부 API(단일 사용자 SPA) 요청을 받아 데이터베이스에 저장하고 조회하는 역할을 합니다.
 *
 * <p>Day 7 Step 8 부터: 캐릭터 만들기 5트랙 외모 선택 분기 추가.
 * <ul>
 *   <li>① ~ ④ 프리셋 트랙 — {@code CharacterPreset.fromImageId} 로 enum 메타에서
 *       {@code appearancePrompt} 만 가져온다. 이미지 생성 호출 없음 (비용 0).</li>
 *   <li>⑤ 커스텀 트랙 — {@code customAppearancePrompt} 로 {@code ImageGenerationService.generate}
 *       1회 호출 → 생성된 이미지 정적 경로를 {@code characterImageUrl} 에 박는다 (비용 1회 발생).</li>
 * </ul>
 * 비용 가드 ({@code ImageDailyQuotaGuard}) 는 {@code ImageGenerationService} 내부에서 동작하므로
 * 이 서비스 레이어에서 별도로 호출할 필요가 없다 (책임 분리).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션 적용 (성능 및 안전성 최적화)
public class SoulmateService {

    private final SoulmateRepository soulmateRepository;
    private final SoulmateAchievementRepository achievementRepository;
    private final ImageGenerationService imageGenerationService;

    /**
     * 새로운 이성친구를 생성하고 데이터베이스에 저장합니다.
     *
     * <p>Day 7 Step 8 — 5트랙 외모 선택 분기:
     * <ol>
     *   <li>{@code characterImageId == "custom"} → 커스텀 트랙: {@code customAppearancePrompt}
     *       필수 검증 → {@code ImageGenerationService.generate} 호출 → 생성 경로 박기.</li>
     *   <li>그 외 → 프리셋 4 트랙: {@code CharacterPreset} 메타에서 {@code appearancePrompt}
     *       조회 (잘못된 ID 면 {@link ErrorCode#SOULMATE_INVALID_PRESET}).</li>
     * </ol>
     *
     * @param request 프론트엔드에서 전달받은 생성 정보 (성별, 캐릭터 ID, 성격, 취미 등)
     * @return 생성된 이성친구의 정보 (Response DTO)
     */
    @Transactional // 데이터를 변경(INSERT)하므로 쓰기 가능한 트랜잭션 적용
    public SoulmateResponse createSoulmate(SoulmateCreateRequest request) {
        // 1. 5트랙 외모 선택 분기 — 트랙별로 appearancePrompt + characterImageUrl 결정
        String appearancePrompt;
        String characterImageUrl;

        if (CharacterPreset.CUSTOM_IMAGE_ID.equals(request.characterImageId())) {
            // ⑤ 커스텀 트랙 — 외모 prompt 검증 + 이미지 1회 생성
            String customPrompt = request.customAppearancePrompt();
            if (customPrompt == null || customPrompt.isBlank()) {
                throw new BusinessException(ErrorCode.SOULMATE_CUSTOM_PROMPT_REQUIRED);
            }
            String fileNameHint = "soulmate-portrait-" + UUID.randomUUID();
            ImageGenerationResult result = imageGenerationService.generate(
                    customPrompt, null, null, fileNameHint);
            appearancePrompt = customPrompt;
            characterImageUrl = result.localPath();
            log.info("[SoulmateService] custom portrait generated: localPath={}, costUsd={}",
                    result.localPath(), result.estimatedCostUsd());
        } else {
            // ① ~ ④ 프리셋 트랙 — enum 메타에서 외모 prompt 만 가져오고 이미지 URL 은 클라이언트가 보낸 정적 경로 그대로
            CharacterPreset preset = CharacterPreset.fromImageId(request.characterImageId());
            appearancePrompt = preset.getAppearancePrompt();
            characterImageUrl = request.characterImageUrl();
        }

        // 2. 트랙 분기 결과로 엔티티 빌드 + DB 저장
        Soulmate soulmate = request.toEntity(appearancePrompt, characterImageUrl);
        Soulmate saved = soulmateRepository.save(soulmate);

        // 3. 저장된 엔티티를 응답 DTO로 변환하여 반환
        return SoulmateResponse.from(saved);
    }

    /**
     * 특정 ID의 이성친구 프로필 상세 정보를 조회합니다.
     * 이 때 해당 이성친구가 획득한 뱃지(업적) 목록도 함께 조회하여 반환합니다.
     * @param id 조회할 이성친구 PK
     * @return 호감도, 레벨, 뱃지가 포함된 프로필 상세 정보
     */
    public SoulmateProfileResponse getSoulmate(Long id) {
        // 1. ID로 이성친구 엔티티 조회 (없으면 커스텀 예외 발생)
        Soulmate soulmate = soulmateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOULMATE_NOT_FOUND));

        // 2. 해당 이성친구가 획득한 뱃지 목록을 최신순으로 조회
        List<String> badges = achievementRepository.findBySoulmateIdOrderByEarnedAtDesc(id)
                .stream()
                .map(SoulmateAchievement::getBadgeCode) // 뱃지 코드(문자열)만 추출
                .toList();

        // 3. 엔티티와 뱃지 목록을 합쳐서 상세 응답 DTO 생성
        return SoulmateProfileResponse.of(soulmate, badges);
    }

    /**
     * 전체 이성친구 목록을 조회합니다.
     */
    public List<SoulmateResponse> getSoulmates() {
        return soulmateRepository.findAll().stream()
                .map(SoulmateResponse::from)
                .toList();
    }
}
