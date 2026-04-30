package kr.spartaclub.aifriends.image.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Day 7 Step 5 — 이미지 모델별 1회 호출 추정 비용을 로그로 에코하고 USD 값을 돌려주는 헬퍼.
 *
 * <p><b>실제 청구 금액이 아니다.</b> 학생이 "텍스트 호출 대비 이미지 호출이 얼마나 비싼지" 감을 잡도록
 * 강의용 하드코딩 매핑을 제공한다. 운영에서는 프로바이더 응답 메타데이터(예: OpenAI {@code usage})
 * 또는 별도 cost-tracker 서비스로 대체해야 한다.</p>
 *
 * <p>비교 기준 (2026-04 시점 공개 가격):
 * <ul>
 *   <li>Pollinations.ai (flux): 무료 (0.0)</li>
 *   <li>OpenAI DALL-E 3 standard 1024×1024: ≈ $0.04</li>
 *   <li>OpenAI DALL-E 3 HD 1024×1024: ≈ $0.08</li>
 *   <li>Google Imagen 3: ≈ $0.04 (Vertex AI 기준, 참고)</li>
 * </ul>
 * 텍스트 LLM 한 번 ≈ $0.0001~$0.001 수준이므로 이미지 1장 = 텍스트 호출 수십~수백 회 분량이다.</p>
 */
@Slf4j
@Component
public class ImageCostEstimator {

    private static final Map<String, Double> COST_USD_PER_CALL = Map.of(
            "pollinations-flux", 0.0,
            "openai-dall-e-3-standard", 0.04,
            "openai-dall-e-3-hd", 0.08,
            "google-imagen-3", 0.04
    );

    public void echo(String modelName) {
        double cost = estimateCostUsd(modelName);
        log.info("[ImageGenerationCost] model={} estimated_cost_usd={}", modelName, cost);
    }

    public double estimateCostUsd(String modelName) {
        return COST_USD_PER_CALL.getOrDefault(modelName, 0.0);
    }
}
