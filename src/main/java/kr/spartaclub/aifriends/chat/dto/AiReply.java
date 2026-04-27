package kr.spartaclub.aifriends.chat.dto;

import java.util.List;

/**
 * Day 4 Step 5 — 소꿉친구 AI 의 한 턴 응답을 타입 안전하게 담는 record.
 *
 * <p>{@link kr.spartaclub.aifriends.chat.service.SoulmateChatService} 가 ChatClient 의
 * {@code .call().entity(AiReply.class)} 로 받아 그대로 흘려 보낸다.
 * BeanOutputConverter 가 이 record 를 분석해 JSON Schema 를 자동 생성하고,
 * 사용자 프롬프트 끝에 format 지시문을 자동 주입해 LLM 응답 형식을 강제한다.</p>
 *
 * <p>레거시 {@code GeminiService} 의 {@code GeminiParsedResponse} 와 필드 구성이 동일하지만,
 * 이름은 프로바이더 중립적인 {@code AiReply} 로 짓는다 — Day 4 이후 도메인 코드가
 * 더 이상 특정 프로바이더(Gemini)에 묶이지 않는다는 신호.</p>
 *
 * @param aiMessage      캐릭터가 플레이어에게 하는 한 턴 대사 (화면에 그대로 표시)
 * @param choices        플레이어가 선택할 수 있는 다음 발화/행동 후보 (보통 2~3개)
 * @param affectionDelta 이 응답으로 캐릭터의 호감도가 변하는 양 (-5 ~ +5 정수)
 */
public record AiReply(
        String aiMessage,
        List<String> choices,
        int affectionDelta
) { }
