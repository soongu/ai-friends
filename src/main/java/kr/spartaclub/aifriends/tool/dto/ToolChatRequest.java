package kr.spartaclub.aifriends.tool.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Day 11 Step 2 — Tool Calling 데모 컨트롤러의 요청 바디.
 *
 * <p>학생이 화면에서 도시명만 입력하면 컨트롤러가 LLM 에게
 * "{city} 의 오늘 날씨에 맞춘 옷차림을 추천해줘" 같은 프롬프트를 던지고,
 * LLM 은 등록된 {@code @Tool} 함수 (WeatherTool.getCurrentWeather) 를 자동 호출해
 * 그 결과를 캐릭터 톤으로 가공한 응답을 돌려준다.</p>
 *
 * @param city 날씨를 물어볼 도시명. 빈 문자열 금지.
 */
public record ToolChatRequest(
        @NotBlank(message = "도시명을 입력해 주세요.")
        String city
) { }
