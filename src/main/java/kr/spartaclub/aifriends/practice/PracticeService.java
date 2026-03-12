package kr.spartaclub.aifriends.practice;

import kr.spartaclub.aifriends.practice.dto.ActivityResponse;
import kr.spartaclub.aifriends.practice.dto.PostResponse;
import kr.spartaclub.aifriends.practice.dto.UserResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * S2S 연습용 서비스 — JSONPlaceholder, Bored API 호출.
 * RestClient는 config/RestClientConfig에서 빈으로 주입 (테스트 시 MockWebServer URL로 대체 가능).
 */
@Service
public class PracticeService {

    /** JSONPlaceholder용 RestClient — GET /posts/{id}, /users/{id} 호출에 사용 */
    private final RestClient jsonPlaceholderClient;
    /** Bored API용 RestClient — GET /random 호출에 사용 */
    private final RestClient boredClient;

    public PracticeService(
            @Qualifier("jsonPlaceholderRestClient") RestClient jsonPlaceholderClient,
            @Qualifier("boredRestClient") RestClient boredClient) {
        this.jsonPlaceholderClient = jsonPlaceholderClient;
        this.boredClient = boredClient;
    }

    /**
     * JSONPlaceholder에서 단일 게시글 조회.
     *
     * @param id 게시글 id (1~100)
     * @return PostResponse (userId, id, title, body)
     */
    public PostResponse getPost(Long id) {
        return jsonPlaceholderClient.get()  // GET 요청 빌더 시작
                .uri("/posts/{id}", id)     // path variable 치환 후 URI 설정
                .retrieve()               // 요청 실행 후 응답 추출 (4xx/5xx 시 예외)
                .body(PostResponse.class); // JSON 본문을 PostResponse로 역직렬화
    }

    /**
     * JSONPlaceholder에서 단일 사용자 조회.
     *
     * @param id 사용자 id (1~10)
     * @return UserResponse (id, name, username, email, address, company 등)
     */
    public UserResponse getUser(Long id) {
        return jsonPlaceholderClient.get()  // GET 요청 빌더 시작
                .uri("/users/{id}", id)     // path variable 치환 후 URI 설정
                .retrieve()               // 요청 실행 후 응답 추출 (4xx/5xx 시 예외)
                .body(UserResponse.class); // JSON 본문을 UserResponse로 역직렬화
    }

    /**
     * Bored API (bored-api.appbrewery.com)에서 랜덤 활동 1건 조회. GET /random
     *
     * @return ActivityResponse (activity, availability, type, participants, price, accessibility, duration, kidFriendly, link, key)
     */
    public ActivityResponse getActivity() {
        return boredClient.get()           // GET 요청 빌더 시작
                .uri("/random")             // 랜덤 활동 1건 요청 (문서: GET https://bored-api.appbrewery.com/random)
                .retrieve()               // 요청 실행 후 응답 추출 (4xx/5xx 시 예외)
                .body(ActivityResponse.class); // JSON 본문을 ActivityResponse로 역직렬화
    }
}
