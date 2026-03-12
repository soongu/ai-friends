package kr.spartaclub.aifriends.dto;

import java.util.List;

/**
 * 서비스 레이어에서 Gemini 통신 후 프론트엔드 또는 자체 로직으로 넘기기 위해
 * 원문 텍스트를 파싱(aiMessage, choices 등)한 결과를 담는 내부용 DTO입니다.
 * 외부 API(Gemini) 모델의 응답과, 우리 서버 내부(Controller) 모델을 분리하는 역할을 합니다.
 */
public record GeminiParsedResponse(
        /** 화면에 보여줄 AI의 순수 텍스트 답변 (선택지 문구 제외) */
        String aiMessage,
        
        /** 모델이 생성한 "다지선다 선택지" 목록 (없을 경우 빈 리스트 반환) */
        List<String> choices,
        
        /** AI가 대화 문맥에 따라 평가한 호감도 증감 수치 */
        int affectionDelta
) {
}
