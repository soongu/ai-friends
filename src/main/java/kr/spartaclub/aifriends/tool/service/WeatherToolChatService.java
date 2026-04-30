package kr.spartaclub.aifriends.tool.service;

import kr.spartaclub.aifriends.tool.WeatherTool;
import kr.spartaclub.aifriends.tool.dto.ToolChatResponse;
import kr.spartaclub.aifriends.tool.dto.WeatherInfo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Day 11 Step 2 — Tool Calling 데모 서비스.
 *
 * <p>{@code weatherToolChatClient} 가 이미 {@code defaultTools(weatherTool)} 로
 * 도구를 장착하고 있으므로 호출부는 평소처럼 {@code prompt().user(...).call()} 만 하면 된다 —
 * LLM 이 도구 호출이 필요한지 스스로 판단하고, 호출이 필요하면 Spring AI 가
 * {@code WeatherTool.getCurrentWeather} 를 자동으로 디스패치한 뒤 그 결과를
 * 다시 LLM 에게 흘려 최종 응답을 만들어낸다. 우리 코드는 그 흐름을 알 필요가 없다.</p>
 *
 * <p>응답에는 LLM 의 자연어 답변(aiMessage)뿐 아니라 같은 도시의 stub 날씨 데이터도
 * 함께 끼워준다 — 학생이 "정말 도구가 호출됐는지" 를 응답만 보고 검증할 수 있도록 한
 * 학습용 디버그 신호다. 운영에선 보통 자연어 응답만 내려보낸다.</p>
 */
@Service
public class WeatherToolChatService {

    private final ChatClient weatherToolChatClient;
    private final WeatherTool weatherTool;

    public WeatherToolChatService(ChatClient weatherToolChatClient, WeatherTool weatherTool) {
        this.weatherToolChatClient = weatherToolChatClient;
        this.weatherTool = weatherTool;
    }

    public ToolChatResponse chat(String city) {
        String userPrompt = "오늘 " + city + " 날씨에 맞춰서 옷차림을 추천해줘.";

        String aiMessage = weatherToolChatClient.prompt()
                .user(userPrompt)
                .call()
                .content();

        // 학습용 디버그 신호 — LLM 이 호출했을 도구의 결과를 응답에 함께 노출.
        // 같은 stub 분기를 한 번 더 호출해 학생이 "도구가 정말 이 값으로 답했구나" 를 눈으로 확인할 수 있게 한다.
        WeatherInfo info = weatherTool.getCurrentWeather(city);

        return new ToolChatResponse(
                info.city(),
                aiMessage,
                info.condition(),
                info.temperatureCelsius(),
                info.precipitationChance());
    }
}
