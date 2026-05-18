package kr.spartaclub.aifriends.tool;

import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.repository.SoulmateRepository;
import kr.spartaclub.aifriends.tool.dto.AffinityInfo;
import kr.spartaclub.aifriends.tool.dto.AffinityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Day 11 Step 4 — Tool Calling 의 세 번째 도구. <strong>읽기 전용(read-only) 도구</strong>.
 *
 * <p>Step 2 ({@code WeatherTool}) · Step 3 ({@code GameStateTool}) 와 결을 맞춰
 * 한 단계씩 결합도를 올린다. 본 도구는 {@link Soulmate#getAffectionScore()} 의 누적 호감도를
 * 그대로 조회만 한다 — score 변경/증감은 일부러 도구에서 막아두었다.</p>
 *
 * <p><strong>왜 읽기 전용인가</strong>: Day 11 의 가르침 한 줄 — "도구의 가장 안전한 시작점은
 * 읽기 전용". LLM 이 자율적으로 호출하는 함수에 쓰기 권한을 곧장 주면, 프롬프트 인젝션이나
 * LLM 의 잘못된 판단 한 번이 그대로 DB 변동으로 이어진다. 호감도 가산 같은 상태 변동은
 * 도구 바깥(예: 매 대화 후 자동 가산 로직, admin 엔드포인트) 에서 일어나야 한다.</p>
 *
 * <p>도메인 재사용 결정: ai-friends 코드베이스의 기존 {@link Soulmate} 엔티티에 이미
 * {@code affectionScore} 필드가 존재해 새 엔티티를 만들지 않고 그대로 재사용한다 — 학생이
 * "기존 도메인을 LLM 에 노출하는 도구는 어떤 모양인가" 를 자연스럽게 보게 한다.</p>
 *
 * <p>예외 정책: Step 2 와 동일하게 도구 본체에선 예외를 던지지 않는다 — 미존재 ID 는
 * found=false + 기본값으로 흘려보내 LLM 이 "아직 어색한 사이" 어투로 가공한다.</p>
 */
@Component
public class AffinityTool {

    private static final Logger log = LoggerFactory.getLogger(AffinityTool.class);

    private final SoulmateRepository soulmateRepository;

    public AffinityTool(SoulmateRepository soulmateRepository) {
        this.soulmateRepository = soulmateRepository;
    }

    @Tool(description = "특정 캐릭터(soulmateId) 와 유저 사이의 현재 호감도(0~100 점수 + 라벨) 를 조회한다. "
            + "유저가 '지금 우리 사이 어때?', '나 좋아해?' 같이 둘의 관계를 물어보면 호출하라. "
            + "이 도구는 읽기 전용 — score 를 바꾸지 않는다. found=false 면 아직 어색한 사이라는 신호이니, "
            + "캐릭터가 '음… 우리 아직 잘 모르는 사이지' 같이 자연스럽게 답하면 된다.")
    public AffinityInfo getAffinity(
            @ToolParam(description = "관계를 조회할 캐릭터의 soulmateId")
            Long soulmateId
    ) {
        log.info("[AffinityTool] getAffinity invoked — soulmateId={}", soulmateId);
        return soulmateRepository.findById(soulmateId)
                .map(this::toInfo)
                .orElseGet(() -> AffinityInfo.unknown(soulmateId));
    }

    private AffinityInfo toInfo(Soulmate soulmate) {
        int score = soulmate.getAffectionScore();
        AffinityLevel level = AffinityLevel.from(score);
        return new AffinityInfo(
                true,
                soulmate.getId(),
                soulmate.getName() == null ? "" : soulmate.getName(),
                score,
                level.label());
    }
}
