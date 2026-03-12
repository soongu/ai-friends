package kr.spartaclub.aifriends.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import kr.spartaclub.aifriends.domain.Soulmate;

import java.util.List;

/**
 * 이성친구 생성 요청 데이터를 담는 DTO(Data Transfer Object)입니다.
 * 프론트엔드(Client)에서 전달된 JSON 데이터를 이 레코드로 바인딩합니다.
 * Java 14+부터 지원되는 record 키워드를 사용하여 불변(Immutable) 데이터 객체를 간결하게 정의합니다.
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
        List<String> speechStyles
) {
        /**
         * DTO를 기반으로 실제 데이터베이스에 저장할 Entity 객체로 변환하는 메서드입니다.
         * 서비스 계층(Service Layer)에서 DTO를 숨기고 도메인 객체만 사용하도록 도와줍니다.
         */
        public Soulmate toEntity() {
                // List<String> 형태의 다중 선택 값을 쉼표(,)로 구분된 단일 문자열로 변환하여 DB에 저장합니다.
                // (나중에 Gemini AI 프롬프트에 제공하기 쉬운 형태)
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
                        this.characterImageUrl,
                        this.name,
                        personalityStr,
                        hobbiesStr,
                        speechStr,
                        0,
                        1,
                        null
                );
        }
}
