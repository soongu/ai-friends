package kr.spartaclub.aifriends.dto;

import kr.spartaclub.aifriends.domain.Soulmate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 이성친구 프로필 상세 조회(+뱃지) 응답 DTO.
 */
public record SoulmateProfileResponse(
        Long id,
        String gender,
        String characterImageId,
        String characterImageUrl,
        String name,
        List<String> personalityKeywords,
        List<String> hobbies,
        List<String> speechStyles,
        Integer affectionScore,
        Integer level,
        List<String> badges,
        LocalDateTime createdAt
) {
    public static SoulmateProfileResponse of(Soulmate entity, List<String> badges) {
        return new SoulmateProfileResponse(
                entity.getId(),
                entity.getGender(),
                entity.getCharacterImageId(),
                entity.getCharacterImageUrl(),
                entity.getName(),
                parseToList(entity.getPersonalityKeywords()),
                parseToList(entity.getHobbies()),
                parseToList(entity.getSpeechStyles()),
                entity.getAffectionScore(),
                entity.getLevel(),
                badges,
                entity.getCreatedAt()
        );
    }

    private static List<String> parseToList(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return List.of();
        }
        return Arrays.stream(commaSeparated.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
