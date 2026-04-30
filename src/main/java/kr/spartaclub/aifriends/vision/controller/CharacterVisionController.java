package kr.spartaclub.aifriends.vision.controller;

import kr.spartaclub.aifriends.common.response.ApiResponse;
import kr.spartaclub.aifriends.vision.dto.SoulmateIntroductionResponse;
import kr.spartaclub.aifriends.vision.service.CharacterVisionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 8 Step 6 — Day 7 portrait 회수의 클로저 (컨트롤러 진입점).
 *
 * <p>캐릭터 ID 를 받아 그 캐릭터의 portrait URL 을 그대로 Vision 입력으로 흘려보낸 결과 —
 * 캐릭터가 자기 자화상을 보고 한 자기소개를 돌려준다. Day 7 에서 만든 portrait URL 이
 * Day 8 의 입력으로 매끈하게 흘러가는 결.</p>
 *
 * <p>응답은 {@link ApiResponse} 로 래핑한다 (§4-1 게이트). 본문 없는
 * {@code POST /api/vision/characters/{soulmateId}/introduce} — body 가 아니라 path variable 로만
 * 트리거한다 (이미 캐릭터에 박힌 portrait URL 을 사용하기 때문).</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/vision")
public class CharacterVisionController {

    private final CharacterVisionService characterVisionService;

    public CharacterVisionController(CharacterVisionService characterVisionService) {
        this.characterVisionService = characterVisionService;
    }

    @PostMapping("/characters/{soulmateId}/introduce")
    public ResponseEntity<ApiResponse<SoulmateIntroductionResponse>> introduce(
            @PathVariable Long soulmateId) {
        SoulmateIntroductionResponse response = characterVisionService.introduce(soulmateId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
