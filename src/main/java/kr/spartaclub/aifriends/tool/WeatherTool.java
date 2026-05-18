package kr.spartaclub.aifriends.tool;

import kr.spartaclub.aifriends.tool.dto.WeatherInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Day 11 Step 2 — Tool Calling 의 첫 번째 도구.
 *
 * <p>{@code @Tool} 어노테이션이 붙은 메서드는 {@link org.springframework.ai.chat.client.ChatClient}
 * 의 {@code .tools(...)} / {@code defaultTools(...)} 에 등록되는 순간, LLM 에게 함수 시그니처가
 * JSON Schema 로 노출된다. 사용자 프롬프트에 "도쿄 날씨 알려줘" 같은 의도가 들어오면
 * LLM 이 스스로 {@code getCurrentWeather("도쿄")} 호출을 결정하고, 그 반환값을 받아
 * 자연스러운 응답을 다시 생성한다.</p>
 *
 * <p>본 도구는 학생 실습이 외부 API 키 없이 굴러가도록 stub 응답을 반환한다.
 * "stub 이지만 LLM 입장에선 진짜 함수처럼 보이는 게 학습 포인트" 가 핵심.</p>
 *
 * <p>도구 함수의 예외는 가급적 던지지 않고 정상 응답으로 흘려준다 —
 * LLM 의 retry 루프가 의도치 않게 폭주하는 걸 막기 위함이다.
 * 도시 화이트리스트에 없는 입력은 기본값으로 답해 안전하게 종료시킨다.</p>
 *
 * <p>도구 진입 시 {@code log.info} 한 줄을 남겨, LLM 이 자율 판단으로 이 메서드를
 * 디스패치했는지 학생이 콘솔에서 직접 확인할 수 있게 한다.</p>
 */
@Component
public class WeatherTool {

    private static final Logger log = LoggerFactory.getLogger(WeatherTool.class);

    @Tool(description = "특정 도시의 현재 날씨(하늘 상태, 기온, 강수확률) 를 조회한다. "
            + "사용자가 옷차림·외출 여부·우산 챙기기 같은 결정을 도와달라고 할 때 호출하라.")
    public WeatherInfo getCurrentWeather(
            @ToolParam(description = "날씨를 조회할 도시명. 예: '서울', '부산', '도쿄'")
            String city
    ) {
        log.info("[WeatherTool] getCurrentWeather invoked — city={}", city);
        // 강의용 stub — 실제 기상 API 호출 대신 도시별 고정 응답을 돌려준다.
        // 학생이 "이 자리를 RestClient 호출로 갈아끼우면 진짜 도구가 된다" 는 감을 잡으면 충분.
        return switch (city) {
            case "서울" -> new WeatherInfo("서울", "흐림", 23, 60);
            case "부산" -> new WeatherInfo("부산", "맑음", 27, 10);
            case "제주" -> new WeatherInfo("제주", "비", 21, 80);
            default -> new WeatherInfo(city, "맑음", 22, 20);
        };
    }
}
