package kr.spartaclub.aifriends.image.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Day 7 Step 5 — 캐릭터 초상화 생성 요청 DTO.
 *
 * <p>{@code prompt} 만 필수로 강제하고, {@code stylePreset} 과 {@code seed} 는
 * 선택값으로 둔다. {@code seed} 가 있으면 같은 입력 → 같은 출력이 보장돼
 * "이 캐릭터의 초상화는 항상 이렇게 보인다" 같은 결정론적 시나리오에 쓰인다.</p>
 */
public record PortraitGenerationRequest(
        @NotBlank(message = "이미지 생성을 위한 프롬프트를 입력해 주세요.") String prompt,
        String stylePreset,
        Long seed
) {
}
