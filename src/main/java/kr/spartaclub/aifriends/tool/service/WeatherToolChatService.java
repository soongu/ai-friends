package kr.spartaclub.aifriends.tool.service;

import kr.spartaclub.aifriends.tool.dto.ToolChatResponse;
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
 * <p>운영 코드 그대로 — 호출부는 자연어 답변({@code aiMessage}) 한 줄만 받아 내린다.
 * 도구가 실제로 호출됐는지의 증거는 {@code WeatherTool} 의 {@code log.info} 로 남는다.</p>
 */
@Service
public class WeatherToolChatService {

    private final ChatClient weatherToolChatClient;

    public WeatherToolChatService(ChatClient weatherToolChatClient) {
        this.weatherToolChatClient = weatherToolChatClient;
    }

    public ToolChatResponse chat(String city) {
        String userPrompt = "오늘 " + city + " 날씨에 맞춰서 옷차림을 추천해줘.";

        String aiMessage = weatherToolChatClient.prompt()
                .user(userPrompt)
                .call()
                .content();

        return new ToolChatResponse(city, aiMessage);
    }
}
