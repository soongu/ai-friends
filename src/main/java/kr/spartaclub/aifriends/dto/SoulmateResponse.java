package kr.spartaclub.aifriends.dto;

import kr.spartaclub.aifriends.domain.Soulmate;

import java.time.LocalDateTime;

/**
 * AI 이성친구 응답/노출용 DTO.
 * 정적 팩토리 {@link #from(Soulmate)} 로 엔티티에서 생성.
 */
public record SoulmateResponse(
        /** PK */
        Long id,
        /** 성별 (예: MALE, FEMALE) */
        String gender,
        /** 캐릭터 이미지 식별자 (선택지 코드 또는 URL 키) */
        String characterImageId,
        /** 표시용 캐릭터 이미지 URL */
        String characterImageUrl,
        /** 표시 이름 (캐릭터명 또는 사용자 지정) */
        String name,
        /** 성격 키워드 (구분자 또는 JSON 배열 문자열) */
        String personalityKeywords,
        /** 취미 (구분자 또는 JSON 배열 문자열) */
        String hobbies,
        /** 말투 스타일 (구분자 또는 JSON 배열 문자열) */
        String speechStyles,
        /** 호감도 누적값 (대화 시 증가) */
        Integer affectionScore,
        /** 레벨 (1~10) */
        Integer level,
        /** 생성 시각 */
        LocalDateTime createdAt
) {
    /** 엔티티로부터 Response 생성 (팩토리메서드에서 사용할 생성자만 사용) */
    public static SoulmateResponse from(Soulmate entity) {
        return new SoulmateResponse(
                entity.getId(),
                entity.getGender(),
                entity.getCharacterImageId(),
                entity.getCharacterImageUrl(),
                entity.getName(),
                entity.getPersonalityKeywords(),
                entity.getHobbies(),
                entity.getSpeechStyles(),
                entity.getAffectionScore(),
                entity.getLevel(),
                entity.getCreatedAt()
        );
    }
}
