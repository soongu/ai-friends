/**
 * 캐릭터 생성 옵션 데이터 — 성별별 이미지, 성격/취미/말투 (이모지+라벨), 캐릭터 메타.
 * 확장 시 여기만 수정한다.
 *
 *  - 보정③: OTHER 성별은 디자인에 없으므로 클라이언트에서는 노출하지 않는다.
 *    서버 enum 자체는 보존(기존 OTHER 데이터 호환). 만약 서버에서 OTHER 캐릭터를
 *    내려주면 갤러리는 fallback 으로 동작 (CHARACTER_DEFS 의 default 매핑).
 */

/** 성별별 캐릭터 이미지: thumb = Step 2 카드 썸네일, avatarUrl = 채팅 헤더/LOG 아바타 */
export const CHARACTER_IMAGES_BY_GENDER = {
  FEMALE: [
    { id: 'character-female-bright', url: '/images/characters/character-female-bright-thumb.jpg', avatarUrl: '/images/characters/character-female-bright-face.jpg' },
    { id: 'character-female-warm',   url: '/images/characters/character-female-warm-thumb.jpg',   avatarUrl: '/images/characters/character-female-warm-face.jpg' },
  ],
  MALE: [
    { id: 'character-male-calm',     url: '/images/characters/character-male-calm-thumb.jpg',     avatarUrl: '/images/characters/character-male-calm-face.jpg' },
    { id: 'character-male-cheerful', url: '/images/characters/character-male-cheerful-thumb.jpg', avatarUrl: '/images/characters/character-male-cheerful-face.jpg' },
  ],
};

/**
 * 캐릭터 메타 — themeKey(디자인 토큰), 디폴트 이름(사용자 미입력 시), 무드 한 줄.
 * 디자이너의 sample name 은 사용자가 이름을 비워둘 때만 fallback 으로 쓴다.
 */
export const CHARACTER_DEFS = {
  'character-female-bright':   { themeKey: 'bright',   name: '지유', mood: '한낮 옥상 카페' },
  'character-female-warm':     { themeKey: 'warm',     name: '하린', mood: '가을 베이커리' },
  'character-male-calm':       { themeKey: 'calm',     name: '준오', mood: '비 오는 도서관' },
  'character-male-cheerful':   { themeKey: 'cheerful', name: '시우', mood: '노을 농구장' },
};

/** 성격: id, emoji, label */
export const PERSONALITY_OPTIONS = [
  { id: 'kind',      emoji: '😊', label: '친절한' },
  { id: 'funny',     emoji: '😂', label: '유머러스한' },
  { id: 'calm',      emoji: '😌', label: '차분한' },
  { id: 'sensitive', emoji: '🥹', label: '감성적' },
  { id: 'active',    emoji: '🔥', label: '활발한' },
  { id: 'reliable',  emoji: '💪', label: '든든한' },
  { id: 'talkative', emoji: '💬', label: '수다스러운' },
  { id: 'quiet',     emoji: '🤫', label: '조용한' },
];

/** 취미 */
export const HOBBIES_OPTIONS = [
  { id: 'movie',  emoji: '🎬', label: '영화' },
  { id: 'music',  emoji: '🎵', label: '음악' },
  { id: 'food',   emoji: '🍽', label: '맛집' },
  { id: 'travel', emoji: '✈️', label: '여행' },
  { id: 'book',   emoji: '📖', label: '독서' },
  { id: 'game',   emoji: '🎮', label: '게임' },
  { id: 'photo',  emoji: '📷', label: '사진' },
  { id: 'sport',  emoji: '⚽', label: '운동' },
];

/** 말투 */
export const SPEECH_OPTIONS = [
  { id: 'casual',   emoji: '💬', label: '반말' },
  { id: 'formal',   emoji: '🙏', label: '존댓말' },
  { id: 'ellipsis', emoji: '⋯', label: '말줄임 많이' },
  { id: 'emoji',    emoji: '😀', label: '이모티콘 자주' },
  { id: 'praise',   emoji: '👍', label: '칭찬 많이' },
  { id: 'question', emoji: '❓', label: '질문 많이' },
];

/** Step 타이틀 (헤더 우측 큰 글자) */
export const STEP_TITLES = {
  1: 'Create New Soulmate',
  2: 'Select Character',
  3: 'Personality · Hobby · Speech',
  4: 'Check and Create',
};

/** Step Eyebrow (헤더 우측 작은 모노 글자) */
export const STEP_EYEBROWS = {
  1: '01 / 04 · NAME & GENDER',
  2: '02 / 04 · CHARACTER',
  3: '03 / 04 · TRAITS',
  4: '04 / 04 · CONFIRM',
};

/** characterImageId → CHARACTER_DEFS 항목 (없으면 default = bright) */
export function getCharacterDef(characterImageId) {
  return CHARACTER_DEFS[characterImageId] || CHARACTER_DEFS['character-female-bright'];
}
