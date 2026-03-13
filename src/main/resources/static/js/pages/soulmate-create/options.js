/**
 * 캐릭터 생성 옵션 데이터 — 성별별 이미지, 성격/취미/말투 (이모지+라벨)
 * 확장 시 여기만 수정
 */

/** 성별별 캐릭터 이미지: 전신(썸네일) = 선택 화면 표시, avatarUrl = 목록/아바타용 얼굴 */
export const CHARACTER_IMAGES_BY_GENDER = {
  FEMALE: [
    { id: 'character-female-bright', url: '/images/characters/character-female-bright-thumb.jpg', avatarUrl: '/images/characters/character-female-bright-face.jpg' },
    { id: 'character-female-warm', url: '/images/characters/character-female-warm-thumb.jpg', avatarUrl: '/images/characters/character-female-warm-face.jpg' },
  ],
  MALE: [
    { id: 'character-male-calm', url: '/images/characters/character-male-calm-thumb.jpg', avatarUrl: '/images/characters/character-male-calm-face.jpg' },
    { id: 'character-male-cheerful', url: '/images/characters/character-male-cheerful-thumb.jpg', avatarUrl: '/images/characters/character-male-cheerful-face.jpg' },
  ],
  OTHER: [
    { id: 'character-female-bright', url: '/images/characters/character-female-bright-thumb.jpg', avatarUrl: '/images/characters/character-female-bright-face.jpg' },
    { id: 'character-male-cheerful', url: '/images/characters/character-male-cheerful-thumb.jpg', avatarUrl: '/images/characters/character-male-cheerful-face.jpg' },
  ],
};

/** 성격: id, emoji, label (hover 시 표시) */
export const PERSONALITY_OPTIONS = [
  { id: 'kind', emoji: '😊', label: '친절한' },
  { id: 'funny', emoji: '😂', label: '유머러스한' },
  { id: 'calm', emoji: '😌', label: '차분한' },
  { id: 'sensitive', emoji: '🥹', label: '감성적' },
  { id: 'active', emoji: '🔥', label: '활발한' },
  { id: 'reliable', emoji: '💪', label: '든든한' },
  { id: 'talkative', emoji: '💬', label: '수다스러운' },
  { id: 'quiet', emoji: '🤫', label: '조용한' },
];

/** 취미 */
export const HOBBIES_OPTIONS = [
  { id: 'movie', emoji: '🎬', label: '영화' },
  { id: 'music', emoji: '🎵', label: '음악' },
  { id: 'food', emoji: '🍽', label: '맛집' },
  { id: 'travel', emoji: '✈️', label: '여행' },
  { id: 'book', emoji: '📖', label: '독서' },
  { id: 'game', emoji: '🎮', label: '게임' },
  { id: 'photo', emoji: '📷', label: '사진' },
  { id: 'sport', emoji: '⚽', label: '운동' },
];

/** 말투 */
export const SPEECH_OPTIONS = [
  { id: 'casual', emoji: '💬', label: '반말' },
  { id: 'formal', emoji: '🙏', label: '존댓말' },
  { id: 'ellipsis', emoji: '⋯', label: '말줄임 많이' },
  { id: 'emoji', emoji: '😀', label: '이모티콘 자주' },
  { id: 'praise', emoji: '👍', label: '칭찬 많이' },
  { id: 'question', emoji: '❓', label: '질문 많이' },
];

export const STEP_TITLES = {
  1: 'Create New Soulmate',
  2: 'Select Character',
  3: 'Personality·Hobby·Speech',
  4: 'Check and Create',
};
