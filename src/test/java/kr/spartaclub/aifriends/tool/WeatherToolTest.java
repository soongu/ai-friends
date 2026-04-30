package kr.spartaclub.aifriends.tool;

import kr.spartaclub.aifriends.tool.dto.WeatherInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Day 11 Step 2 — Tool Calling 의 첫 번째 도구 {@link WeatherTool} 단위 테스트.
 *
 * <p>이 테스트는 {@code @Tool} 어노테이션이 LLM 으로부터 호출될 때
 * 도구가 반환할 stub 값이 의도한 분기에 맞게 나오는지 확인한다.
 * 학생 실습 환경에는 실제 기상 API 키가 없으므로 도구 자체를 stub 으로 박아두고,
 * "LLM 이 이 함수를 호출하면 이런 값을 돌려준다" 만 검증한다.</p>
 *
 * <p>실제 LLM 이 도구를 호출했는지 여부는 통합 테스트({@code ToolCallingControllerTest})
 * 또는 강사의 수동 smoke 에서 확인한다. 단위 테스트는 도구 본체의 결정론적 동작만 검증한다.</p>
 */
class WeatherToolTest {

    private final WeatherTool weatherTool = new WeatherTool();

    @Test
    @DisplayName("getCurrentWeather - 서울이면 흐림 23도 강수확률 60% stub 을 반환한다")
    void getCurrentWeather_seoul_returnsCloudyStub() {
        WeatherInfo info = weatherTool.getCurrentWeather("서울");

        assertThat(info.city()).isEqualTo("서울");
        assertThat(info.condition()).isEqualTo("흐림");
        assertThat(info.temperatureCelsius()).isEqualTo(23);
        assertThat(info.precipitationChance()).isEqualTo(60);
    }

    @Test
    @DisplayName("getCurrentWeather - 부산이면 맑음 27도 강수확률 10% stub 을 반환한다")
    void getCurrentWeather_busan_returnsSunnyStub() {
        WeatherInfo info = weatherTool.getCurrentWeather("부산");

        assertThat(info.city()).isEqualTo("부산");
        assertThat(info.condition()).isEqualTo("맑음");
        assertThat(info.temperatureCelsius()).isEqualTo(27);
        assertThat(info.precipitationChance()).isEqualTo(10);
    }

    @Test
    @DisplayName("getCurrentWeather - 등록되지 않은 도시면 기본값(맑음 22도 강수확률 20%) 을 반환한다")
    void getCurrentWeather_unknownCity_returnsDefault() {
        WeatherInfo info = weatherTool.getCurrentWeather("아틀란티스");

        assertThat(info.city()).isEqualTo("아틀란티스");
        assertThat(info.condition()).isEqualTo("맑음");
        assertThat(info.temperatureCelsius()).isEqualTo(22);
        assertThat(info.precipitationChance()).isEqualTo(20);
    }
}
