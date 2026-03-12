package kr.spartaclub.aifriends.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import kr.spartaclub.aifriends.common.exception.ErrorResponse;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorResponse error) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> fail(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }
}
