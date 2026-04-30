package kr.spartaclub.aifriends.tool.dto;

/**
 * Day 11 Step 2 — {@link kr.spartaclub.aifriends.tool.WeatherTool} 의 반환 타입.
 *
 * <p>Spring AI 가 LLM 에 도구 스키마를 만들어 보낼 때 이 record 의 필드 이름·타입을
 * JSON Schema 로 자동 변환한다. 학생이 이름만 직관적으로 지어도 LLM 이 그 의미를 읽어준다.</p>
 *
 * <p>실제 기상 API 호출 없이 stub 값만 채워서 학생 실습을 외부 키 의존에서 자유롭게 만든다 —
 * 이 강의의 학습 포인트는 "@Tool 어노테이션 → ChatClient 가 자동 호출" 흐름이지,
 * 실제 기상 데이터 정확도가 아니다.</p>
 *
 * @param city                 도시명 (요청 그대로 echo)
 * @param condition            하늘 상태 ("맑음", "흐림", "비" 등)
 * @param temperatureCelsius   섭씨 온도
 * @param precipitationChance  강수 확률 (0~100, 퍼센트)
 */
public record WeatherInfo(
        String city,
        String condition,
        int temperatureCelsius,
        int precipitationChance
) { }
