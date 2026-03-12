package kr.spartaclub.aifriends.repository;

import kr.spartaclub.aifriends.domain.SoulmateAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * SoulmateAchievement 엔티티 JPA Repository.
 */
public interface SoulmateAchievementRepository extends JpaRepository<SoulmateAchievement, Long> {

    /** Soulmate별 획득 뱃지 목록 (획득 시각 순) */
    List<SoulmateAchievement> findBySoulmateIdOrderByEarnedAtDesc(Long soulmateId);

    /** 특정 Soulmate가 특정 뱃지를 이미 획득했는지 여부 확인 */
    boolean existsBySoulmateIdAndBadgeCode(Long soulmateId, String badgeCode);
}
