package kr.spartaclub.aifriends.dto;

import java.util.List;

/**
 * AI의 답변과 갱신된 캐릭터 상태(호감도, 레벨 등)를 프론트엔드로 반환하는 종합 응답 DTO입니다.
 *
 * <p>Day 7 Step 9 부터: 셀카 요청 응답 시 {@code imageUrl} 필드에 정적 리소스 경로가 박힌다
 * (예: {@code /uploads/portraits/selca-xxx.jpg}). 일반 채팅 응답이거나 한도 초과로 우회된 경우 {@code null}.</p>
 */
public record AiChatResponse(
        /** 이번 턴에 사용자가 보냈던 메시지 (화면 동기화 혹은 확인용) */
        String userMessage,

        /** 모델이 생성한 순수 텍스트 답변 (선택지 문구 제외) */
        String aiMessage,

        /** 미연시 게임 스타일의 다지선다 선택지 목록. 없으면 빈 리스트를 반환합니다. */
        List<String> choices,

        /** 어떤 캐릭터와 대화했는지 식별하기 위한 ID */
        Long soulmateId,

        /** 이번 대화를 통해 갱신된 현재 호감도 수치 */
        Integer affectionScore,

        /** 이번 대화를 통해 갱신된 현재 레벨 */
        Integer level,

        /** 이번 턴에 새롭게 획득한 업적(뱃지)의 코드 목록. 없으면 빈 리스트. */
        List<String> newBadges,

        /**
         * 셀카 응답 시 캐릭터 셀카 이미지의 정적 리소스 경로 (Day 7 Step 9).
         * 일반 채팅이거나 한도 초과/생성 실패로 우회된 응답이면 {@code null}.
         */
        String imageUrl
) {
    @Override
    public String toString() {
        return "AiChatResponse[userMessage=*** (%d chars), aiMessage=*** (%d chars), choices=%s, soulmateId=%s, affectionScore=%s, level=%s, newBadges=%s, imageUrl=%s]".formatted(
                userMessage == null ? 0 : userMessage.length(),
                aiMessage == null ? 0 : aiMessage.length(),
                choices, soulmateId, affectionScore, level, newBadges, imageUrl);
    }
}
