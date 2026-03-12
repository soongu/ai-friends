package kr.spartaclub.aifriends.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 대화 기록 엔티티 (발화 1건당 1행).
 * 단방향: Soulmate 엔티티 참조 없이 soulmateId(FK) 컬럼만 보관.
 */
@Entity
@Table(name = "chat_log", indexes = {
        @Index(name = "idx_chat_log_soulmate_created", columnList = "soulmate_id, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChatLog {

    /** PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 Soulmate ID (FK) */
    @Column(nullable = false)
    private Long soulmateId;

    /** 발화 주체: USER, AI */
    @Column(nullable = false, length = 10)
    private String speaker;

    /** 메시지 내용 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** 발화 시각 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
