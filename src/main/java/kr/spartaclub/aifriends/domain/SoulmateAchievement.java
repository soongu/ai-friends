package kr.spartaclub.aifriends.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Soulmate별 뱃지(업적) 이력.
 * 단방향: Soulmate 엔티티 참조 없이 soulmateId(FK)만 보관.
 * (soulmateId, badgeCode) 유니크로 동일 뱃지 중복 부여 방지.
 */
@Entity
@Table(name = "soulmate_achievement", uniqueConstraints = {
        @UniqueConstraint(name = "uq_soulmate_badge", columnNames = {"soulmate_id", "badge_code"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SoulmateAchievement {

    /** PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 Soulmate ID (FK) */
    @Column(nullable = false)
    private Long soulmateId;

    /** 뱃지 코드 (업적 식별자) */
    @Column(nullable = false, length = 50)
    private String badgeCode;

    /** 획득 시각 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime earnedAt;

    @PrePersist
    protected void onCreate() {
        if (earnedAt == null) {
            earnedAt = LocalDateTime.now();
        }
    }
}
