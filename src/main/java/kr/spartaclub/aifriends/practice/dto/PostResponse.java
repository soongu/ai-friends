package kr.spartaclub.aifriends.practice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * JSONPlaceholder /posts/{id} 응답 DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PostResponse(
        /** 글 작성자 userId */
        Integer userId,
        /** 글 id */
        Integer id,
        /** 제목 */
        String title,
        /** 본문 */
        String body
) {}
