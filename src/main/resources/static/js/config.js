/**
 * 앱 설정 — 로고 URL, 라우트, 상수
 * React 전환 시 환경 변수 또는 context로 이식
 */
export const CONFIG = {
  /** 로고 이미지 URL. null이면 placeholder(회색 박스) 표시 */
  logoImageUrl: null,

  routes: {
    start: '/soulmate/new',
    chat: (id) => `/chat/${id}`,
    newSoulmate: '/soulmate/new',
  },

  /** 프론트에서만 적용하는 최대 이성친구 수 */
  maxSoulmates: 3,

  copy: {
    taglineLanding: '나만의 AI 소울메이트를 만나보세요',
    taglineSelect: '대화할 소울메이트를 선택하세요',
    ctaStart: '시작하기',
    ctaNew: '새로 만들기',
    maxHint: '최대 3명까지 생성 가능해요',
  },
};

/**
 * 캐릭터 이미지 ID → 추상 voice key (mood) 매핑.
 *
 * 프로바이더(OpenAI/ElevenLabs)에 종속된 식별자를 프론트가 직접 박지 않는다. 대신 캐릭터의
 * "결" 을 표현하는 4 가지 mood key 만 보내고, 실제 프로바이더별 voice id (OpenAI 의 marin/cedar
 * 또는 ElevenLabs 의 UUID) 로 변환하는 책임은 백엔드 {@code VoiceSynthesisService} 가 진다.
 * 그래서 .env 의 {@code TTS_PROVIDER} 만 바꾸면 프론트 코드는 한 줄도 안 건드려도 캐릭터별
 * voice 가 자연스럽게 swap 된다 — Spring AI {@code TextToSpeechModel} 인터페이스 추상화의 효과.
 *
 *  - bright   : 밝고 또랑또랑한 여성 → 한낮 옥상 카페
 *  - warm     : 부드럽고 따뜻한 여성 → 가을 베이커리
 *  - calm     : 깊고 차분한 남성    → 비 오는 도서관
 *  - cheerful : 활기 있는 남성     → 노을 농구장
 *
 * 새 predefined 캐릭터 추가 시 이 표에만 한 줄 추가하면 된다.
 */
const MOOD_BY_CHARACTER = {
  'character-female-bright':   'bright',
  'character-female-warm':     'warm',
  'character-male-calm':       'calm',
  'character-male-cheerful':   'cheerful',
};

/**
 * 매핑 표에 없는 캐릭터 (사용자가 직접 만든 custom 캐릭터 등) 의 mood 폴백.
 *
 * 사용자가 portrait 업로드로 만든 캐릭터는 {@code characterImageId="custom"} 인데, 이게
 * MOOD_BY_CHARACTER 에 없으면 백엔드가 default voice(ElevenLabs Rachel) 로 폴백해
 * *모든 custom 캐릭터가 같은 목소리* 로 들리는 사고가 난다. 폴백은 *gender + id 짝수/홀수*
 * 로 4 mood 에 결정론적으로 분산시켜 — 같은 캐릭터는 항상 같은 voice, 다른 캐릭터는
 * (확률 1/2 로) 다른 voice 가 떨어지도록 한다.
 */
function fallbackMoodByGenderAndId(soulmate) {
  const gender = (soulmate?.gender || '').toUpperCase();
  const id = Number(soulmate?.id) || 0;
  const even = id % 2 === 0;
  if (gender === 'FEMALE') return even ? 'bright' : 'warm';
  if (gender === 'MALE')   return even ? 'calm'   : 'cheerful';
  return even ? 'bright' : 'calm';   // gender 미상 폴백
}

/**
 * 캐릭터의 mood key 를 돌려준다.
 *
 * @param {Object|string} soulmateOrImageId — 권장은 soulmate 객체({id, gender, characterImageId}).
 *        하위 호환을 위해 문자열(characterImageId) 도 받지만, 이 경우 매핑 누락 시 'bright' 로 폴백한다.
 * @returns {string} mood key (항상 4 자루 중 하나 — null 반환하지 않음 → 백엔드 default voice 폴백 방지)
 */
export function pickVoice(soulmateOrImageId) {
  if (typeof soulmateOrImageId === 'string') {
    // 하위 호환: characterImageId 문자열만 받은 경우
    return MOOD_BY_CHARACTER[soulmateOrImageId] || 'bright';
  }
  const soulmate = soulmateOrImageId || {};
  return MOOD_BY_CHARACTER[soulmate.characterImageId] || fallbackMoodByGenderAndId(soulmate);
}
