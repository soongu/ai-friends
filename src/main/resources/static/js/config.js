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
