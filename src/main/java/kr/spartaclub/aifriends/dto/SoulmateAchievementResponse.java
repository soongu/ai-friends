package kr.spartaclub.aifriends.dto;

import kr.spartaclub.aifriends.domain.SoulmateAchievement;

import java.time.LocalDateTime;

/**
 * Soulmate별 뱃지(업적) 1건 응답용 DTO.
 * 정적 팩토리 {@link #from(SoulmateAchievement)} 로 엔티티에서 생성.
 */
public record SoulmateAchievementResponse(
        /** PK */
        Long id,
        /** 소속 Soulmate ID (FK) */
        Long soulmateId,
        /** 뱃지 코드 (업적 식별자) */
        String badgeCode,
        /** 획득 시각 */
        LocalDateTime earnedAt
) {
    /** 엔티티로부터 Response 생성 (팩토리메서드에서 사용할 생성자만 사용) */
    public static SoulmateAchievementResponse from(SoulmateAchievement entity) {
        return new SoulmateAchievementResponse(
                entity.getId(),
                entity.getSoulmateId(),
                entity.getBadgeCode(),
                entity.getEarnedAt()
        );
    }
}
