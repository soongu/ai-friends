package kr.spartaclub.aifriends.chat.service;

import kr.spartaclub.aifriends.chat.dto.SelcaResult;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.image.dto.ImageGenerationResult;
import kr.spartaclub.aifriends.image.exception.ImageException;
import kr.spartaclub.aifriends.image.service.ImageGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Day 7 Step 9 — 챗 셀카 요청 처리 서비스.
 *
 * <p>chat 도메인과 image 도메인의 결합을 한 자리로 모아둔다. {@code SoulmateChatService} 는 LLM 호출만,
 * {@code ImageGenerationService} 는 이미지 생성만 책임지고, 이 서비스가 *셀카 요청* 이라는 *교차 관심사*
 * (키워드 감지 + 외모 일관성 prompt 합성 + 가드 한도 우회 응답) 를 캡슐화한다.</p>
 *
 * <p><strong>키워드 매칭의 한계 — Day 11 Tool Calling 으로 가는 복선</strong>: 본 구현은 단순 키워드
 * 매칭이라 *"셀카 잘 안 나오네"* 같은 메시지에 오감지 가능. 의도 분류 정확도를 올리려면 LLM 의 *intent
 * classification* 호출 1회를 추가해야 하는데, 그것이 곧 Day 11 (Tool Calling) 의 자리이다 — LLM 이
 * *"이 메시지가 selca 요청인가"* 를 도구 호출로 직접 판단하면 키워드 매칭의 한계가 사라진다.</p>
 *
 * <p><strong>가드 한도 초과 우회</strong>: {@code ImageDailyQuotaGuard} 가 {@link ImageException}
 * ({@link ErrorCode#IMAGE_QUOTA_EXCEEDED}) 를 throw 하면 이 서비스가 *캐릭터 인격 톤* 의 우회 메시지로
 * 변환한다. 비용 가드의 *기술적 차단* 이 *캐릭터 인격* 으로 변환되는 미연시 몰입감 자리.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SelcaService {

    /**
     * 셀카 요청 *명령형* 만 매칭 — 단일 키워드 매칭은 LLM choices 의 감상 응답까지 잡아
     * 무한 셀카 루프를 만든다. 키워드(셀카·셀피·사진 등)와 *요청 동사*(보내·찍어·찍자·보여·줘·줄래·찍·보낼래)
     * 가 12자 이내에 결합된 경우만 진짜 요청으로 간주한다.
     *
     * <p>오감지 차단 사례:
     * <ul>
     *   <li>"셀카 이쁘다" — 동사 없음 ⇒ 매칭 안 됨</li>
     *   <li>"사진 잘 나왔어" — 동사 없음 ⇒ 매칭 안 됨</li>
     *   <li>"오늘 사진 너무 좋아" — 동사 없음 ⇒ 매칭 안 됨</li>
     * </ul>
     * 진짜 요청 매칭 사례: "셀카 보내줘", "오늘 카페에서 책 읽는 셀카 보내줘", "사진 한 장 더 줘",
     * "셀피 찍어줘", "이쁜 셀카 보여줘".
     *
     * <p>키워드 매칭의 한계 자체는 그대로 — *"이 사진 한번 봐줘"* 같은 *동사 보유 감상* 은
     * 여전히 오감지 가능하나 빈도 낮음. 진짜 정확도는 Day 11 Tool Calling 의 의도 분류 자리.</p>
     */
    private static final Pattern SELCA_PATTERN = Pattern.compile(
            "(?i)(셀카|셀피|사진|selfie|selca).{0,12}(보내|찍어|찍자|찍|보여|줄래|보낼래|줘\\b|줘$|줘\\s)");

    private static final String QUOTA_EXCEEDED_FALLBACK =
            "오늘 셀카 너무 많이 찍었나봐 ㅠㅠ 카메라 배터리 다 됐어... 내일 또 찍어줄게!";

    private static final String GENERATION_FAILED_FALLBACK =
            "셀카 보내려고 했는데 핸드폰이 말썽이네 ㅠㅠ 다음에 다시 시도해보자!";

    private final ImageGenerationService imageGenerationService;

    /**
     * 사용자 메시지가 셀카 요청인지 키워드 매칭으로 판단한다.
     * <p>한계는 클래스 javadoc 의 *Day 11 Tool Calling 복선* 박스 참조.</p>
     */
    public boolean isSelcaRequest(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return false;
        return SELCA_PATTERN.matcher(userMessage).find();
    }

    /**
     * 셀카 한 장 생성. 외모 일관성 prompt = {@code Soulmate.appearancePrompt} + 사용자 자유 입력
     * + 셀카 키워드.
     *
     * <p>이미지 생성 호출은 1회. {@code ImageDailyQuotaGuard} · {@code ImageCostEstimator} 는
     * {@code ImageGenerationService} 내부에서 동작하므로 이 자리에서는 결과/예외만 다룬다.</p>
     */
    public SelcaResult generate(Soulmate soulmate, String userMessage) {
        String prompt = composePrompt(soulmate.getAppearancePrompt(), userMessage);
        String fileNameHint = "selca-" + UUID.randomUUID();
        try {
            ImageGenerationResult result = imageGenerationService.generate(
                    prompt, null, null, fileNameHint);
            log.info("[SelcaService] generated selca: localPath={}, costUsd={}",
                    result.localPath(), result.estimatedCostUsd());
            return SelcaResult.success(result.localPath());
        } catch (ImageException e) {
            if (e.getErrorCode() == ErrorCode.IMAGE_QUOTA_EXCEEDED) {
                log.info("[SelcaService] quota exceeded — falling back to character voice");
                return SelcaResult.quotaExceeded(QUOTA_EXCEEDED_FALLBACK);
            }
            log.warn("[SelcaService] image generation failed: {}", e.getMessage());
            return SelcaResult.failed(GENERATION_FAILED_FALLBACK);
        }
    }

    /**
     * 외모 일관성 prompt 합성.
     * <pre>
     * [Soulmate.appearancePrompt]
     * + ", " + [사용자 메시지에서 셀카 키워드 제거한 자유 입력]
     * + ", selfie, casual photo angle"
     * </pre>
     * 사용자 자유 입력이 비어 있으면 (*"셀카 보내줘"* 같은 미지정 요청) 기본 self portrait 만 합성.
     */
    String composePrompt(String appearancePrompt, String userMessage) {
        String userRequest = SELCA_PATTERN.matcher(userMessage).replaceAll("").trim();
        // 흔한 한국어 부속어("보내줘", "찍어줘" 등) 는 그대로 두고 LLM 이 시각화하도록 흘려보낸다 —
        // 학습 측면에서 *prompt 의 자유 입력 부분이 어떻게 시각화되는지* 의 손맛이 보인다.
        StringBuilder sb = new StringBuilder();
        sb.append(appearancePrompt == null ? "" : appearancePrompt);
        if (!userRequest.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(userRequest);
        }
        if (sb.length() > 0) sb.append(", ");
        sb.append("selfie, casual photo angle");
        return sb.toString();
    }
}
