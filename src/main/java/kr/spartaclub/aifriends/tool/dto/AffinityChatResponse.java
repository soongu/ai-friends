package kr.spartaclub.aifriends.tool.dto;

/**
 * Day 11 Step 4 — {@code POST /api/tool/affinity/chat} 의 응답 바디.
 *
 * <p>{@code aiMessage} 는 캐릭터 톤의 자연어 응답이다. LLM 이 자율 판단으로
 * {@code AffinityTool.getAffinity} 를 호출했는지 여부는 도구 메서드 내부의
 * {@code log.info} 로 콘솔에서 확인한다 — Step 2 {@code ToolChatResponse} 와 같은 결로,
 * 응답 바디는 운영 패턴 그대로 자연어 답변만 담는다.</p>
 */
public record AffinityChatResponse(
        Long soulmateId,
        String aiMessage
) { }
