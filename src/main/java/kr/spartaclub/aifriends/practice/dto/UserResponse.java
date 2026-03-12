package kr.spartaclub.aifriends.practice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * JSONPlaceholder /users/{id} 응답 DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserResponse(
        /** 사용자 id */
        Integer id,
        /** 이름 */
        String name,
        /** 로그인 id */
        String username,
        /** 이메일 */
        String email,
        /** 주소 */
        AddressResponse address,
        /** 전화번호 */
        String phone,
        /** 웹사이트 */
        String website,
        /** 소속 회사 */
        CompanyResponse company
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AddressResponse(
            String street,
            String suite,
            String city,
            String zipcode,
            GeoResponse geo
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeoResponse(String lat, String lng) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CompanyResponse(String name, String catchPhrase, String bs) {}
}
