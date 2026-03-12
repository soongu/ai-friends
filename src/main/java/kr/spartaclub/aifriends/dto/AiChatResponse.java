package kr.spartaclub.aifriends.dto;

import java.util.List;

/**
 * AI의 답변과 갱신된 캐릭터 상태(호감도, 레벨 등)를 프론트엔드로 반환하는 종합 응답 DTO입니다.
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
        List<String> newBadges
) {
}
