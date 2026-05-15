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

    /**
     * 외모 일관성 prompt (Day 7 Step 8 추가).
     * <p>캐릭터 만들기 시 박힌 외모 묘사. 챗 셀카 요청(Step 9) 시 사용자 요청(포즈/표정/장소) 앞에
     * 합성되어 *같은 캐릭터의 외모* 를 셀카마다 유지한다. 프리셋 트랙은 {@code CharacterPreset}
     * 의 메타가, 커스텀 트랙은 사용자 입력이 그대로 박힌다. nullable 인 이유는 Day 7 이전에
     * 만들어진 기존 데이터(레거시 row)와의 호환성 때문이다.</p>
     */
    @Column(length = 1000)
    private String appearancePrompt;

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
