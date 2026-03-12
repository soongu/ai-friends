package kr.spartaclub.aifriends.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * AI 이성친구 엔티티.
 * 연관관계는 단방향만 사용 — ChatLog, SoulmateAchievement 쪽에서 soulmateId(FK)만 보관하고
 * 이 엔티티에서는 컬렉션 참조하지 않음.
 */
@Entity
@Table(name = "soulmate")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Soulmate {

    /** PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 성별 (예: MALE, FEMALE) */
    @Column(nullable = false, length = 20)
    private String gender;

    /** 캐릭터 이미지 식별자 (선택지 코드 또는 URL 키) */
    @Column(nullable = false, length = 100)
    private String characterImageId;

    /** 표시용 캐릭터 이미지 URL */
    @Column(length = 500)
    private String characterImageUrl;

    /** 표시 이름 (캐릭터명 또는 사용자 지정) */
    @Column(length = 100)
    private String name;

    /** 성격 키워드 (구분자 또는 JSON 배열 문자열) */
    @Column(nullable = false, length = 500)
    private String personalityKeywords;

    /** 취미 (구분자 또는 JSON 배열 문자열) */
    @Column(nullable = false, length = 500)
    private String hobbies;

    /** 말투 스타일 (구분자 또는 JSON 배열 문자열) */
    @Column(nullable = false, length = 500)
    private String speechStyles;

    /** 호감도 누적값 (대화 시 증가) */
    @Column(nullable = false)
    private Integer affectionScore = 0;

    /** 레벨 (1~10) */
    @Column(nullable = false)
    private Integer level = 1;

    /** 생성 시각 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** 호감도 증감 (AI가 계산한 값을 적용, 최소 0 유지) */
    public void addAffection(int delta) {
        this.affectionScore = Math.max(0, this.affectionScore + delta);
    }

    /** 레벨 설정 (레벨업 시 서비스에서 호출) */
    public void setLevel(int level) {
        this.level = Math.min(10, Math.max(1, level));
    }
}
