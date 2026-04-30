package kr.spartaclub.aifriends.tool.dto;

/**
 * Day 11 Step 2 — Tool Calling 데모의 응답 바디.
 *
 * <p>{@code aiMessage} 는 캐릭터가 학생에게 들려줄 한 턴 대사,
 * {@code condition·temperatureCelsius·precipitationChance} 는 LLM 이 호출한
 * {@code WeatherTool.getCurrentWeather} 의 결과를 그대로 같이 돌려주는 디버그 신호다.
 * "정말로 도구가 호출됐는지" 를 학생이 응답만 보고 확인할 수 있게 함께 노출한다.</p>
 */
public record ToolChatResponse(
        String city,
        String aiMessage,
        String condition,
        int temperatureCelsius,
        int precipitationChance
) { }
