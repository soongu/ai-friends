package kr.spartaclub.aifriends.tool.dto;

/**
 * Day 11 Step 4 — 호감도(score 0~100) 를 사람이 읽기 쉬운 라벨로 변환하는 enum.
 *
 * <p>도구 함수가 LLM 에게 "score=60" 같은 숫자만 던져도 LLM 이 알아서 자연어로 가공할 수는
 * 있다. 다만 라벨까지 함께 흘려주면 캐릭터 톤의 응답이 훨씬 안정적으로 나온다 — "단짝"
 * 이라는 명시적 신호가 있으면 LLM 이 "우리 단짝 정도지" 같은 어투를 일관되게 유지한다.</p>
 *
 * <p>경계값은 4구간으로 단순하게 박았다 — 강의용으로는 4단계 분기면 충분하고, 실무에선
 * 운영 데이터에 맞춰 더 세분화하면 된다. 정적 팩토리 {@link #from(int)} 한 곳에서만
 * score → 라벨 매핑이 일어나도록 분리해 둔다 (도구 본체에 분기 흩뿌리지 않기 위함).</p>
 */
public enum AffinityLevel {
    STRANGER("낯선 사이"),
    FRIEND("친구"),
    BESTIE("단짝"),
    LOVER("연인");

    private final String label;

    AffinityLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * score(0~100) 를 4구간으로 나눠 라벨을 결정한다.
     * <ul>
     *   <li>0~24: 낯선 사이</li>
     *   <li>25~49: 친구</li>
     *   <li>50~74: 단짝</li>
     *   <li>75~100: 연인</li>
     * </ul>
     * 0 미만은 STRANGER, 100 초과는 LOVER 로 클램프한다.
     */
    public static AffinityLevel from(int score) {
        if (score < 25) {
            return STRANGER;
        }
        if (score < 50) {
            return FRIEND;
        }
        if (score < 75) {
            return BESTIE;
        }
        return LOVER;
    }
}
