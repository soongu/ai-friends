package kr.spartaclub.aifriends.tool.dto;

/**
 * Day 11 Step 4 — {@code POST /api/tool/affinity/chat} 의 응답 바디.
 *
 * <p>{@code aiMessage} 는 캐릭터 톤의 자연어 응답이고, {@code score · level} 은 LLM 이
 * 도구로부터 실제로 받은 호감도 정보를 디버그 신호로 같이 노출한다. Step 2 의
 * {@code ToolChatResponse} 가 weather 정보를 함께 노출했던 것과 동일한 결 —
 * "도구가 정말 호출됐는지" 응답만 보고 학생이 검증할 수 있게 한다.</p>
 */
public record AffinityChatResponse(
        Long soulmateId,
        String aiMessage,
        int score,
        String level
) { }
