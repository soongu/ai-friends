package kr.spartaclub.aifriends.domain;

import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;

/**
 * Day 7 Step 8 — 캐릭터 만들기 시 선택 가능한 프리셋 4종.
 *
 * <p>각 프리셋은 {@code static/images/characters/character-{id}-thumb.jpg} 의
 * 시각 정체성과 일치하는 한국어 + 영어 보강 키워드로 구성된 {@code appearancePrompt} 를
 * 가진다. 이 prompt 는 두 자리에서 사용된다:
 * <ol>
 *   <li>캐릭터 만들기 시 — 프리셋 선택 트랙은 {@code appearancePrompt} 를 그대로 박고
 *       이미지 생성을 호출하지 않는다 (비용 0).</li>
 *   <li>챗 셀카 요청 시 — 외모 일관성 유지를 위해 {@code appearancePrompt} 를
 *       사용자 요청(포즈/표정/장소) 앞에 합성한다 (Step 9).</li>
 * </ol>
 *
 * <p>한국어 시각화 + 영어 키워드 보강은 의도적이다. 학생이 prompt 를 한국어로 읽고
 * 손에 익히되, Pollinations.ai 의 결과 일관성(애니풍 일러스트, 한국인 얼굴 락)을 위한
 * 영어 키워드 헷지를 끝에 둔다. 영어 변환 자리는 Day 11 (Tool Calling) 또는
 * Day 15 (RAG) 의 prompt engineering 자리에서 다시 만난다.</p>
 */
public enum CharacterPreset {

    MALE_CHEERFUL(
            "male-cheerful",
            """
            20대 후반 한국인 남성, 짧고 자연스럽게 헝클어진 다크브라운 머리, 옅은 회청색 눈, \
            부드러운 옅은 미소, 운동을 좋아하는 듯한 다정하고 활기찬 인상, 하늘색 라운드 셔츠. \
            anime portrait illustration, soft cel shading, korean male, late 20s, \
            messy short dark brown hair, friendly cheerful smile, light blue t-shirt"""
    ),

    MALE_CALM(
            "male-calm",
            """
            30대 초반 한국인 남성, 옆가르마로 정돈된 다크브라운 머리, 짙은 갈색 눈, \
            차분하고 옅은 표정, 지적이고 부드러운 분위기, 검은색 셔츠. \
            anime portrait illustration, soft cel shading, korean male, early 30s, \
            side-parted dark brown hair, calm gentle expression, black shirt"""
    ),

    FEMALE_WARM(
            "female-warm",
            """
            20대 후반 한국인 여성, 어깨 아래까지 내려오는 긴 갈색 스트레이트 머리, 따뜻한 갈색 눈, \
            부드럽고 다정한 미소, 단아하고 포근한 인상, 아이보리색 니트 스웨터. \
            anime portrait illustration, soft cel shading, korean female, late 20s, \
            long straight brown hair, warm gentle smile, ivory knit sweater"""
    ),

    FEMALE_BRIGHT(
            "female-bright",
            """
            20대 초반 한국인 여성, 어깨 길이 단발의 살짝 구불구불한 다크브라운 머리, 큰 갈색 눈, \
            환하고 발랄한 미소, 작은 귀걸이, 발랄하고 생기 넘치는 캠퍼스 분위기, 민트색 라운드 티. \
            anime portrait illustration, soft cel shading, korean female, early 20s, \
            wavy short bob dark brown hair, bright lively smile, mint green t-shirt"""
    );

    /** 커스텀 트랙을 가리키는 sentinel — {@code SoulmateCreateRequest.characterImageId} 가 이 값이면 5번째 트랙. */
    public static final String CUSTOM_IMAGE_ID = "custom";

    private final String characterImageId;
    private final String appearancePrompt;

    CharacterPreset(String characterImageId, String appearancePrompt) {
        this.characterImageId = characterImageId;
        this.appearancePrompt = appearancePrompt;
    }

    public String getCharacterImageId() {
        return characterImageId;
    }

    public String getAppearancePrompt() {
        return appearancePrompt;
    }

    /**
     * 프리셋 ID 로 enum 을 조회한다. 4 프리셋 중 어느 것도 매칭되지 않으면
     * {@link BusinessException} 으로 래핑한다 (커스텀 트랙은 호출 전에 분기되어
     * 이 메서드까지 오지 않는다).
     *
     * <p>입력 ID 는 두 결을 모두 받는다 — 프론트엔드 기존 옵션 데이터는 {@code character-male-cheerful}
     * 처럼 *{@code character-} 접두사* 가 박힌 결이고, Day 7 신설 5트랙 시안 마크업은 *접두사 없는*
     * {@code male-cheerful} 결이다. 두 결을 모두 매칭시켜 마이그레이션 부담을 0 으로 둔다.</p>
     */
    public static CharacterPreset fromImageId(String characterImageId) {
        if (characterImageId == null) {
            throw new BusinessException(ErrorCode.SOULMATE_INVALID_PRESET);
        }
        String normalized = characterImageId.startsWith("character-")
                ? characterImageId.substring("character-".length())
                : characterImageId;
        for (CharacterPreset preset : values()) {
            if (preset.characterImageId.equals(normalized)) {
                return preset;
            }
        }
        throw new BusinessException(ErrorCode.SOULMATE_INVALID_PRESET);
    }
}
