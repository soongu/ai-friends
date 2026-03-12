package kr.spartaclub.aifriends.service;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.domain.SoulmateAchievement;
import kr.spartaclub.aifriends.dto.SoulmateCreateRequest;
import kr.spartaclub.aifriends.dto.SoulmateProfileResponse;
import kr.spartaclub.aifriends.dto.SoulmateResponse;
import kr.spartaclub.aifriends.repository.SoulmateAchievementRepository;
import kr.spartaclub.aifriends.repository.SoulmateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 이성친구(Soulmate) 관련 비즈니스 로직을 처리하는 서비스입니다.
 * 외부 API(단일 사용자 SPA) 요청을 받아 데이터베이스에 저장하고 조회하는 역할을 합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션 적용 (성능 및 안전성 최적화)
public class SoulmateService {

    private final SoulmateRepository soulmateRepository;
    private final SoulmateAchievementRepository achievementRepository;

    /**
     * 새로운 이성친구를 생성하고 데이터베이스에 저장합니다.
     * @param request 프론트엔드에서 전달받은 생성 정보 (성별, 성격, 취미 등)
     * @return 생성된 이성친구의 정보 (Response DTO)
     */
    @Transactional // 데이터를 변경(INSERT)하므로 쓰기 가능한 트랜잭션 적용
    public SoulmateResponse createSoulmate(SoulmateCreateRequest request) {
        // 1. 요청 DTO를 JPA 엔티티로 변환
        Soulmate soulmate = request.toEntity();
        
        // 2. DB에 저장
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
