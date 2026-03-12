package kr.spartaclub.aifriends.practice;

import kr.spartaclub.aifriends.common.response.ApiResponse;
import kr.spartaclub.aifriends.practice.dto.ActivityResponse;
import kr.spartaclub.aifriends.practice.dto.PostResponse;
import kr.spartaclub.aifriends.practice.dto.UserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * S2S 연습용 API — JSONPlaceholder, Bored API 프록시.
 */
@RestController
@RequestMapping("/api/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @GetMapping("/post/{id}")
    public ApiResponse<PostResponse> getPost(@PathVariable Long id) {
        return ApiResponse.success(practiceService.getPost(id));
    }

    @GetMapping("/user/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        return ApiResponse.success(practiceService.getUser(id));
    }

    @GetMapping("/activity")
    public ApiResponse<ActivityResponse> getActivity() {
        return ApiResponse.success(practiceService.getActivity());
    }
}
