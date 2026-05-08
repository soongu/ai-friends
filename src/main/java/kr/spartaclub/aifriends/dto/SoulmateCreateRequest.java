package kr.spartaclub.aifriends.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import kr.spartaclub.aifriends.domain.Soulmate;

import java.util.List;

/**
 * 이성친구 생성 요청 데이터를 담는 DTO(Data Transfer Object)입니다.
 * 프론트엔드(Client)에서 전달된 JSON 데이터를 이 레코드로 바인딩합니다.
 * Java 14+부터 지원되는 record 키워드를 사용하여 불변(Immutable) 데이터 객체를 간결하게 정의합니다.
 *
 * <p>Day 7 Step 8 부터: {@code customAppearancePrompt} 필드 추가. 5트랙 외모 선택의 트랙 ⑤
 * (커스텀 외모) 일 때 외모 묘사를 담는다. 프리셋 4 트랙(① male-cheerful · ② male-calm ·
 * ③ female-warm · ④ female-bright) 일 때는 null 이며, 서비스 레이어가 {@code CharacterPreset}
 * 의 메타에서 외모 prompt 를 가져온다.</p>
 */
public record SoulmateCreateRequest(
        // @NotBlank: null이 아니고, 공백 문자열("")이나 공백으로만 이루어진 문자열("  ")이 아님을 검증합니다.
        @NotBlank(message = "성별을 입력해 주세요")
        String gender,

        @NotBlank(message = "캐릭터 이미지를 선택해 주세요")
        String characterImageId,

        // 필수값이 아닌 필드는 별도의 검증 어노테이션을 붙이지 않습니다.
        String characterImageUrl,

        String name,

        // @NotEmpty: 컬렉션(List 등)이 null이 아니고 비어있지 않음(size > 0)을 검증합니다.
        @NotEmpty(message = "성격 키워드를 1개 이상 선택해 주세요")
        List<String> personalityKeywords,

        @NotEmpty(message = "취미를 1개 이상 선택해 주세요")
        List<String> hobbies,

        @NotEmpty(message = "말투 스타일을 1개 이상 선택해 주세요")
        List<String> speechStyles,

        /**
         * 커스텀 트랙(⑤) 전용 — 사용자 입력 외모 묘사. 프리셋 4 트랙일 때는 null.
         * 검증은 서비스 레이어({@code SoulmateService}) 가 트랙 분기와 함께 수행한다
         * — DTO 단계에서는 트랙 정체를 모르기에 {@code @NotBlank} 같은 단순 검증으로는
         * 표현할 수 없는 *조건부 필수* 라서.
         */
        String customAppearancePrompt
) {
        /**
         * DTO를 기반으로 실제 데이터베이스에 저장할 Entity 객체로 변환하는 메서드입니다.
         * 서비스 계층(Service Layer)에서 DTO를 숨기고 도메인 객체만 사용하도록 도와줍니다.
         *
         * <p>Day 7 Step 8 부터: 외모 일관성 prompt 와 캐릭터 이미지 URL 은 *서비스 레이어*
         * 가 5트랙 분기 후 결정한 값을 인자로 넘긴다 (DTO 가 트랙 정체를 모르므로).</p>
         *
         * @param appearancePrompt    트랙별 결정된 외모 묘사 (프리셋이면 enum 메타, 커스텀이면 사용자 입력)
         * @param characterImageUrl   트랙별 결정된 이미지 URL (프리셋이면 정적 리소스, 커스텀이면 /uploads/...)
         */
        public Soulmate toEntity(String appearancePrompt, String characterImageUrl) {
                // List<String> 형태의 다중 선택 값을 쉼표(,)로 구분된 단일 문자열로 변환하여 DB에 저장합니다.
                String personalityStr = String.join(",", personalityKeywords);
                String hobbiesStr = String.join(",", hobbies);
                String speechStr = String.join(",", speechStyles);

                // Builder 패턴 대신 생성자를 사용하여 엔티티를 생성합니다.
                // ID는 DB의 AUTO_INCREMENT로 자동 생성되므로 null,
                // 호감도는 기본값 0, 레벨은 기본값 1, 생성일시는 JPA `@PrePersist`에서 자동 할당되므로 null을 전달합니다.
                return new Soulmate(
                        null,
                        this.gender,
                        this.characterImageId,
                        characterImageUrl,
                        this.name,
                        personalityStr,
                        hobbiesStr,
                        speechStr,
                        0,
                        1,
                        null,
                        appearancePrompt
                );
        }
}
