package kr.spartaclub.aifriends.tool.dto;

/**
 * Day 11 Step 2 — Tool Calling 데모의 응답 바디.
 *
 * <p>{@code aiMessage} 는 캐릭터가 학생에게 들려줄 한 턴 대사다.
 * LLM 이 자율 판단으로 {@code WeatherTool.getCurrentWeather} 를 호출했는지 여부는
 * 도구 메서드 내부의 {@code log.info} 로 콘솔에서 확인한다 — 응답 바디는 운영 패턴 그대로
 * 자연어 답변만 담는다.</p>
 */
public record ToolChatResponse(
        String city,
        String aiMessage
) { }
