/**
 * API 일괄 관리 — 백엔드 REST 호출
 * React 전환 시 axios/fetch 래퍼 또는 React Query로 이식
 */

const BASE = '';

/**
 * @typedef {Object} ApiResponse
 * @property {boolean} success
 * @property {T} [data]
 * @property {{ message?: string, code?: string }} [error]
 */

/**
 * @template T
 * @param {string} url
 * @param {RequestInit} [init]
 * @returns {Promise<ApiResponse<T>>}
 */
async function request(url, init = {}) {
  const res = await fetch(`${BASE}${url}`, {
    headers: {
      'Content-Type': 'application/json',
      ...init.headers,
    },
    ...init,
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    const msg = body?.error?.message || body?.message || res.statusText;
    throw new Error(msg || `HTTP ${res.status}`);
  }
  return body;
}

/** 소울메이트 생성 요청 Body (SoulmateCreateRequest) */
/**
 * @typedef {Object} SoulmateCreateBody
 * @property {string} gender
 * @property {string} characterImageId
 * @property {string} [characterImageUrl]
 * @property {string} [name]
 * @property {string[]} personalityKeywords
 * @property {string[]} hobbies
 * @property {string[]} speechStyles
 */

/** 소울메이트 생성 응답 (SoulmateResponse) */
/**
 * @typedef {Object} SoulmateResponse
 * @property {number} id
 * @property {string} gender
 * @property {string} characterImageId
 * @property {string} [characterImageUrl]
 * @property {string} [name]
 * @property {string} personalityKeywords
 * @property {string} hobbies
 * @property {string} speechStyles
 * @property {number} affectionScore
 * @property {number} level
 * @property {string} createdAt
 */

/**
 * 소울메이트 생성
 * @param {SoulmateCreateBody} body
 * @returns {Promise<SoulmateResponse>}
 */
export async function createSoulmate(body) {
  const res = await request('/api/soulmates', {
    method: 'POST',
    body: JSON.stringify(body),
  });
  if (!res.success || res.data == null) throw new Error(res?.error?.message || '생성에 실패했어요');
  return res.data;
}

/**
 * 전체 소울메이트 목록 조회
 * @returns {Promise<SoulmateResponse[]>}
 */
export async function getSoulmates() {
  const res = await request('/api/soulmates');
  if (!res.success || res.data == null) throw new Error(res?.error?.message || '목록 조회에 실패했어요');
  const list = res.data.soulmates;
  return Array.isArray(list) ? list : [];
}

/**
 * 소울메이트 단건(프로필) 조회
 * @param {number} id
 * @returns {Promise<Object>} 프로필 데이터 (SoulmateProfileResponse)
 */
export async function getSoulmate(id) {
  const res = await request(`/api/soulmates/${id}`);
  if (!res.success || res.data == null) throw new Error(res?.error?.message || '조회에 실패했어요');
  return res.data;
}

/** 채팅 API 응답 (AiChatResponse) */
/**
 * @typedef {Object} AiChatResponse
 * @property {string} userMessage
 * @property {string} aiMessage
 * @property {string[]} choices
 * @property {number} soulmateId
 * @property {number} affectionScore
 * @property {number} level
 * @property {string[]} [newBadges]
 */

/**
 * AI 채팅 전송
 * @param {number} soulmateId
 * @param {string} userMessage
 * @returns {Promise<AiChatResponse>}
 */
export async function postChat(soulmateId, userMessage) {
  const res = await request('/api/chat', {
    method: 'POST',
    body: JSON.stringify({ soulmateId, userMessage }),
  });
  if (!res.success || res.data == null) throw new Error(res?.error?.message || '전송에 실패했어요');
  return res.data;
}

/**
 * 대화 히스토리 페이징 조회 (최신순 DESC, page 0이 가장 최신)
 * @param {number} soulmateId
 * @param {number} [page=0]
 * @param {number} [size=30]
 * @returns {Promise<{ content: Array<{ id: number, soulmateId: number, speaker: string, message: string, createdAt: string }>, hasNext: boolean }>}
 */
export async function getChatLogs(soulmateId, page = 0, size = 30) {
  const res = await request(`/api/soulmates/${soulmateId}/chat/logs?page=${page}&size=${size}`);
  if (!res.success || res.data == null) throw new Error(res?.error?.message || '대화 기록을 불러올 수 없어요');
  const slice = res.data;
  const content = Array.isArray(slice.content) ? slice.content : [];
  const hasNext = slice.last === false;
  return { content, hasNext };
}
