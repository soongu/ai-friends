/**
 * 캐릭터 생성 옵션 데이터 — 성별별 이미지, 성격/취미/말투 (이모지+라벨)
 * 확장 시 여기만 수정
 */

/** 성별별 캐릭터 이미지 목록. 이미지 추가 시 배열만 확장 */
export const CHARACTER_IMAGES_BY_GENDER = {
  FEMALE: [
    { id: 'female-1', url: 'https://picsum.photos/seed/f1/400/400' },
    { id: 'female-2', url: 'https://picsum.photos/seed/f2/400/400' },
  ],
  MALE: [
    { id: 'male-1', url: 'https://picsum.photos/seed/m1/400/400' },
    { id: 'male-2', url: 'https://picsum.photos/seed/m2/400/400' },
  ],
  OTHER: [
    { id: 'other-1', url: 'https://picsum.photos/seed/o1/400/400' },
    { id: 'other-2', url: 'https://picsum.photos/seed/o2/400/400' },
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
  1: '새 이성친구 만들기',
  2: '캐릭터 선택',
  3: '성격·취미·말투',
  4: '확인하고 만들기',
};
