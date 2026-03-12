package kr.spartaclub.aifriends.practice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Bored API (bored-api.appbrewery.com) GET /random 응답 DTO — 랜덤 활동 추천.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ActivityResponse(
        /** 활동 설명 */
        String activity,
        /** 이용 가능도 (0.0 ~ 1.0) */
        Double availability,
        /** 활동 유형 (education, recreational 등) */
        String type,
        /** 참여 인원 수 */
        Integer participants,
        /** 비용 (0.0 ~ 1.0) */
        Double price,
        /** 접근성 설명 (예: "Few to no challenges") */
        String accessibility,
        /** 소요 시간 (예: "hours") */
        String duration,
        /** 아동 친화 여부 */
        Boolean kidFriendly,
        /** 관련 링크 (없으면 null 또는 빈 문자열) */
        String link,
        /** 활동 고유 키 */
        String key
) {}
