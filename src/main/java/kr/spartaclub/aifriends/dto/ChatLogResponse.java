package kr.spartaclub.aifriends.dto;

import kr.spartaclub.aifriends.domain.ChatLog;

import java.time.LocalDateTime;

/**
 * 대화 기록 1건 응답용 DTO.
 * 정적 팩토리 {@link #from(ChatLog)} 로 엔티티에서 생성.
 */
public record ChatLogResponse(
        /** PK */
        Long id,
        /** 소속 Soulmate ID (FK) */
        Long soulmateId,
        /** 발화 주체: USER, AI */
        String speaker,
        /** 메시지 내용 */
        String message,
        /** 발화 시각 */
        LocalDateTime createdAt
) {
    /** 엔티티로부터 Response 생성 (팩토리메서드에서 사용할 생성자만 사용) */
    public static ChatLogResponse from(ChatLog entity) {
        return new ChatLogResponse(
                entity.getId(),
                entity.getSoulmateId(),
                entity.getSpeaker(),
                entity.getMessage(),
                entity.getCreatedAt()
        );
    }
}
