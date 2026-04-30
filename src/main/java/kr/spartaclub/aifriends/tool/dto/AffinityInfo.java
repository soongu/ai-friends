package kr.spartaclub.aifriends.tool.dto;

/**
 * Day 11 Step 4 — {@link kr.spartaclub.aifriends.tool.AffinityTool#getAffinity} 의 반환 DTO.
 *
 * <p>Step 3 의 {@code GameStateSnapshot} 과 같은 결로 {@code found} 를 명시적 boolean 으로 둔다 —
 * 신규 캐릭터(저장된 호감도가 0 인 경우) 는 found=false 로 흘려주면 LLM 이 "아직 어색한 사이"
 * 같은 어투로 자연스럽게 가공한다. null 반환은 LLM 입장에서 다루기 까다로워 항상 객체로 넘긴다.</p>
 *
 * @param found         Soulmate 가 DB 에 존재하면 true, 없으면 false (이때 score=0, level="낯선 사이")
 * @param soulmateId    조회한 캐릭터 ID — 응답에 그대로 반사해 디버그 추적을 쉽게 한다
 * @param characterName 캐릭터 표시 이름 (없으면 빈 문자열)
 * @param score         호감도 누적값 (0~100)
 * @param level         사람이 읽기 좋은 라벨 — "낯선 사이" / "친구" / "단짝" / "연인"
 */
public record AffinityInfo(
        boolean found,
        Long soulmateId,
        String characterName,
        int score,
        String level
) {
    /**
     * 신규 / 미존재 soulmateId 에 대한 일관된 기본 응답.
     * "아직 만난 적 없는 캐릭터" 도 LLM 입장에선 정상 응답이라는 결을 살린다.
     */
    public static AffinityInfo unknown(Long soulmateId) {
        return new AffinityInfo(false, soulmateId, "", 0, AffinityLevel.STRANGER.label());
    }
}
