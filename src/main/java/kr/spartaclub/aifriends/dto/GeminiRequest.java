package kr.spartaclub.aifriends.dto;

import java.util.List;
import java.util.Map;

/**
 * Gemini API에 텍스트 생성을 요청(POST)할 때 사용하는 JSON 데이터를 담는 DTO입니다.
 * 외부 서비스 형태에 맞춰 레코드(record) 기반의 불변 객체로 설계되었습니다.
 */
public record GeminiRequest(
        // 시스템 레벨의 지시문 (페르소나, 행동 지침, 응답 형식 등)
        SystemInstruction systemInstruction,
        
        // 실제 대화 맥락 (이전 대화 기록 + 현재 사용자 입력)
        List<Content> contents,
        
        // 생성 옵션 (창의도, 최대 길이 등)
        GenerationConfig generationConfig
) {
    /**
     * 페르소나 및 시스템 지침을 담는 객체
     * Gemini API는 system_instruction 필드 안에 parts의 배열 형태로 지시문을 받습니다.
     */
    public record SystemInstruction(
            List<Part> parts
    ) {}

    /**
     * 하나의 발화 턴(User 또는 Model)을 나타내는 객체
     * 대화형 AI의 핵심인 "멀티 턴(Multi-turn)" 대화를 유지하기 위해 사용됩니다.
     */
    public record Content(
            // "user" (사용자 발화) 또는 "model" (AI 발화)
            String role,
            
            // 발화의 상세 내용
            List<Part> parts
    ) {}

    /**
     * 실제 텍스트 내용을 담는 객체
     * 텍스트 외에 이미지(Blob)나 파일 데이터가 들어갈 수도 있도록 API 구조가 확장성을 가집니다.
     * 우리 서비스는 텍스트만 다룹니다.
     */
    public record Part(
            String text
    ) {}

    /**
     * 답변 생성과 관련된 설정값 (temperature, 최대 토큰 등)
     * 이 값들을 조절하여 AI의 답변 스타일(얼마나 일관될지, 얼마나 창의적일지)을 통제합니다.
     * responseMimeType + responseJsonSchema 를 주면 Gemini가 해당 스키마에 맞는 JSON만 반환합니다.
     */
    public record GenerationConfig(
            // 0.0 ~ 2.0. 낮을수록 정답에 가까운 고정된 답변, 높을수록 창의적이고 다양한 답변을 생성합니다.
            Double temperature,
            
            // 모델이 생성할 수 있는 최대 토큰(단어 조각) 수. 너무 긴 답변을 방지합니다.
            Integer maxOutputTokens,
            
            // 토큰 샘플링 임계값. 1.0에 가까울수록 다양한 어휘를, 낮을수록 확률이 높은 어휘만 선택합니다.
            Double topP,
            
            // 상위 N개의 확률 토큰 풀 안에서 샘플링합니다. 낮을수록 엉뚱한 답변이 나올 확률이 줄어듭니다.
            Integer topK,
            
            // "application/json" 이면 응답 본문이 스키마에 맞는 JSON 문자열로 고정됩니다.
            String responseMimeType,
            
            // JSON 응답일 때 필수. JSON Schema 객체 (type, properties, required 등). null이면 무시됩니다.
            Map<String, Object> responseJsonSchema
    ) {}
}
