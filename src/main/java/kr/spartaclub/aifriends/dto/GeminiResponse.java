package kr.spartaclub.aifriends.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Gemini API로부터 반환받은 응답 JSON을 매핑할 DTO입니다.
 * 필요한 필드(답변 텍스트, 차단 여부 등)만 파싱하도록 선언되어 있습니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(
        List<Candidate> candidates,
        PromptFeedback promptFeedback
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(
            Content content,
            String finishReason
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(
            String role,
            List<Part> parts
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(
            String text
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PromptFeedback(
            String blockReason
    ) {
    }

    /**
     * 응답에서 실제 채팅 메시지 본문 텍스트만 추출하는 편의 메서드입니다.
     * 안전 필터 등에 의해 candidates가 비어있거나 올바르지 않으면 null을 반환합니다.
     */
    public String extractText() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        Candidate candidate = candidates.get(0);
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            return null;
        }
        return candidate.content().parts().get(0).text();
    }
}
