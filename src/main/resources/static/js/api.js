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
 * Day 7 Step 8 — 캐릭터 만들기 *커스텀 트랙* 의 미리보기/컨펌 흐름.
 * Step 6 에서 깔아둔 학습용 lab 컨트롤러 (POST /api/images/portraits) 를 prod 의 *외모 미리보기* 자리에서 재활용한다.
 * 응답의 localPath 를 SoulmateCreateRequest.characterImageUrl 에 박아 createSoulmate 호출하면 백엔드는 ImageGenerationService 를 재호출하지 않는다.
 *
 * @param {string} prompt — 사용자 입력 외모 묘사
 * @returns {Promise<{localPath: string, externalUrl: string, prompt: string, modelName: string, estimatedCostUsd: number}>}
 */
export async function generatePortrait(prompt) {
  const res = await request('/api/images/portraits', {
    method: 'POST',
    body: JSON.stringify({ prompt, stylePreset: null, seed: null }),
  });
  if (!res.success || res.data == null) throw new Error(res?.error?.message || '이미지 생성에 실패했어요');
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

/**
 * 소울메이트 삭제 — 연관 대화/뱃지까지 캐스케이드 삭제 (서버 측 처리).
 * 응답 본문은 비어 있으며 성공 여부는 ApiResponse.success 로만 판단한다.
 * @param {number} id
 * @returns {Promise<void>}
 */
export async function deleteSoulmate(id) {
  const res = await request(`/api/soulmates/${id}`, { method: 'DELETE' });
  if (!res.success) throw new Error(res?.error?.message || '삭제에 실패했어요');
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
 * STT — 마이크 녹음 Blob 을 보내 인식된 텍스트를 받는다.
 * multipart/form-data 의 audio 파트로 전송 (Content-Type 헤더는 브라우저가 자동 설정).
 * @param {Blob} audioBlob — MediaRecorder 의 녹음 결과 (audio/webm 권장)
 * @param {string} [filename='recording.webm']
 * @returns {Promise<string>} 인식된 텍스트 (빈 문자열 가능)
 */
export async function transcribeAudio(audioBlob, filename = 'recording.webm') {
  const form = new FormData();
  form.append('audio', audioBlob, filename);
  const res = await fetch(`${BASE}/api/voice/transcribe`, { method: 'POST', body: form });
  const body = await res.json().catch(() => ({}));
  if (!res.ok || !body.success || body.data == null) {
    const msg = body?.error?.message || body?.message || res.statusText;
    throw new Error(msg || '음성 인식에 실패했어요');
  }
  return body.data.text || '';
}

/**
 * TTS — 텍스트를 받아 합성된 audio/mpeg Blob + 메타데이터를 돌려준다.
 * 응답이 binary 라 request() 헬퍼가 아니라 fetch 직접 사용.
 * 백엔드가 X-TTS-Provider / X-TTS-Voice 헤더를 박아 주니 어떤 프로바이더로 swap 됐는지
 * 한 사이클 호출만으로도 확인 가능 — DevTools Network 탭과 콘솔 로그 양쪽에서.
 *
 * @param {string} text
 * @param {string} [voice] — 추상 mood key (bright/warm/calm/cheerful) 또는 raw voice id.
 *                          null/undefined 면 모델 기본값.
 * @returns {Promise<{blob: Blob, provider: string, voice: string}>}
 */
export async function synthesizeSpeech(text, voice) {
  const body = voice ? { text, voice } : { text };
  const res = await fetch(`${BASE}/api/voice/speak`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    // 에러는 JSON (ApiResponse.fail)
    const body = await res.json().catch(() => ({}));
    const msg = body?.error?.message || body?.message || res.statusText;
    throw new Error(msg || '음성 합성에 실패했어요');
  }
  const blob = await res.blob();
  const provider = res.headers.get('X-TTS-Provider') || '';
  const resolvedVoice = res.headers.get('X-TTS-Voice') || '';
  console.info('[TTS]', { provider, voice: resolvedVoice, requestedVoice: voice || null, bytes: blob.size });
  return { blob, provider, voice: resolvedVoice };
}

/**
 * 활성 TTS 프로바이더 조회 — DevTools 헤더만으로 부족할 때 단독 확인용.
 * @returns {Promise<string>} provider 식별자 (예: "openai", "elevenlabs")
 */
export async function getVoiceInfo() {
  const res = await request('/api/voice/info');
  return res?.data?.provider || '';
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
