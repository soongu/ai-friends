/**
 * 채팅 화면 — 미연시 정석 A 패러다임 (단일 화자 다이얼로그 + LOG 백로그 + VOICE 훅)
 *
 *  - 단일 화자: 다이얼로그 박스에 마지막 한 마디만, 이전 대화는 LOG 모달
 *  - 캐릭터 테마(bright/warm/calm/cheerful) 는 root 의 CSS variable 로 전파
 *  - 음성 입력은 default OFF — VOICE 토글 ON 시 첫 1회 마이크 권한 모달.
 *    실제 STT/TTS 활성화는 Day 9 에서 (이 파일은 hooks only).
 */
import { getSoulmate, postChat, getChatLogs, transcribeAudio, synthesizeSpeech } from '../../api.js';
import { pickVoice } from '../../config.js';
import {
  initBgm,
  setTtsActive,
  pauseBgmForRecording,
  resumeBgmAfterRecording,
  playStartChime,
} from './bgm.js';

// ============================================================
// 캐릭터 메타 매핑 (characterImageId → 디자인 토큰 + 텍스트)
// ============================================================
const CHARACTER_META = {
  'character-female-bright':   { theme: 'bright',   tag: 'BRIGHT',   mood: '한낮 옥상 카페' },
  'character-female-warm':     { theme: 'warm',     tag: 'WARM',     mood: '가을 베이커리' },
  'character-male-calm':       { theme: 'calm',     tag: 'CALM',     mood: '비 오는 도서관' },
  'character-male-cheerful':   { theme: 'cheerful', tag: 'CHEERFUL', mood: '노을 농구장' },
};
const DEFAULT_META = CHARACTER_META['character-female-bright'];

const SELECTORS = {
  root: '[data-app-root]',
  // 무대
  stage: '[data-chat-bg]',
  stageBg: '[data-chat-stage-bg]',
  // 헤더
  status: '[data-chat-status]',
  statusAffection: '[data-chat-status-affection]',
  statusAffectionValue: '[data-chat-status-affection-value]',
  statusLevel: '[data-chat-status-level]',
  hamburger: '[data-chat-hamburger]',
  dropdown: '[data-chat-dropdown]',
  menuHome: '[data-chat-menu-home]',
  menuHistory: '[data-chat-menu-history]',
  soulmateNameText: '[data-chat-soulmate-name-text]',
  soulmateThemeTag: '[data-chat-soulmate-theme-tag]',
  // 다이얼로그
  dialog: '[data-chat-dialog]',
  dialogFrame: '.dialog__frame',
  portrait: '[data-chat-portrait]',
  nametagName: '[data-chat-nametag-name]',
  nametagMood: '[data-chat-nametag-mood]',
  messages: '[data-chat-messages]',
  currentMessage: '[data-chat-current-message]',
  affection: '[data-chat-affection]',
  affectionDelta: '[data-chat-affection-delta]',
  levelUp: '[data-chat-level-up]',
  // 입력
  inputRow: '[data-chat-input-row]',
  input: '[data-chat-input]',
  sendBtn: '[data-chat-send]',
  sendIcon: '[data-chat-send-icon]',
  sendLoading: '[data-chat-send-loading]',
  // 음성 (default OFF — Day 9 에서 활성화)
  voiceToggle: '[data-chat-voice-toggle]',
  voiceRecord: '[data-chat-voice-record]',
  voiceCancel: '[data-chat-voice-cancel]',
  voiceTranscript: '[data-chat-voice-transcript]',
  // 선택지
  choiceModal: '[data-chat-choice-modal]',
  choiceMessage: '[data-chat-choice-message]',
  choiceButtons: '[data-chat-choice-buttons]',
  // LOG 모달
  historyModal: '[data-chat-history-modal]',
  historyBackdrop: '[data-chat-history-backdrop]',
  historyClose: '[data-chat-history-close]',
  historyList: '[data-chat-history-list]',
  historyLoad: '[data-chat-history-load]',
  // 마이크 권한
  micPerm: '[data-mic-permission-modal]',
  micPermAllow: '[data-mic-permission-allow]',
  micPermDefer: '[data-mic-permission-defer]',
};

const HOME_URL = '/';
const HISTORY_PAGE_SIZE = 30;
const MIC_PERM_KEY = 'aifriends.mic.permission'; // 'granted' | 'deferred'

// 상태
let rootEl;
let soulmateId;
let storedProfile = null;
let currentImageId = null;
let currentMeta = DEFAULT_META;
let previousAffectionScore = 0;
let previousLevel = 1;
let voiceMode = false; // VOICE 토글 상태 (default OFF)

// 음성 입출력 — MediaRecorder + 재생 큐
let mediaStream = null;       // getUserMedia 로 받은 마이크 스트림 (한 번 받아두고 재사용)
let mediaRecorder = null;     // 녹음 인스턴스 (stop 시 새로 생성)
let recordedChunks = [];      // dataavailable 누적 버퍼
let isRecording = false;      // 녹음 중 토글
let recordCanceled = false;   // 취소 버튼 → onstop 콜백이 STT 호출하지 않게 가드
let recordStartedAt = 0;      // 녹음 시간 카운터용 timestamp
let recordTimerId = null;     // 1초 간격 mm:ss 갱신
let recordStartTimer = null;  // BGM fade + chime → 녹음 시작 사이의 지연 타이머
let currentReplyAudio = null; // 마지막 재생 중인 <audio>; 새 응답 오면 정지

// 페이지네이션 (LOG 모달)
let historyNextPage = 0;
let historyHasMore = true;
let historyLoading = false;
let historyScrollEl = null;

function $(sel, parent = document) {
  return (parent || rootEl).querySelector(sel);
}
function $$(sel, parent = document) {
  return Array.from((parent || rootEl).querySelectorAll(sel));
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str ?? '';
  return div.innerHTML;
}
function escapeAttr(str) {
  if (!str) return '';
  return escapeHtml(str).replace(/"/g, '&quot;');
}

// ============================================================
// 캐릭터 테마 적용 — root 의 CSS variable 로 모든 자식에 전파
// ============================================================
function getCharacterMeta(characterImageId) {
  return CHARACTER_META[characterImageId] || DEFAULT_META;
}

function applyCharacterTheme(profile) {
  currentImageId = profile.characterImageId;
  currentMeta = getCharacterMeta(currentImageId);
  const theme = currentMeta.theme;

  // 테마 6변수 — 모든 .smcard, .dialog, .choice-card, .summary-card 등이 자동 적용
  rootEl.style.setProperty('--theme-primary',   `var(--theme-${theme}-primary)`);
  rootEl.style.setProperty('--theme-secondary', `var(--theme-${theme}-secondary)`);
  rootEl.style.setProperty('--theme-accent',    `var(--theme-${theme}-accent)`);
  rootEl.style.setProperty('--theme-deep',      `var(--theme-${theme}-deep)`);
  rootEl.style.setProperty('--theme-mist',      `var(--theme-${theme}-mist)`);
  rootEl.style.setProperty('--theme-glow',      `var(--theme-${theme}-glow)`);

  if (currentImageId) {
    rootEl.style.setProperty('--portrait-image', `url('/images/characters/${currentImageId}-face.jpg')`);
  }
  setStageBg('neutral');

  // 텍스트 슬롯
  const displayName = profile.name || '소울메이트';
  const nameText = $(SELECTORS.soulmateNameText);
  const themeTag = $(SELECTORS.soulmateThemeTag);
  const nametagName = $(SELECTORS.nametagName);
  const nametagMood = $(SELECTORS.nametagMood);
  if (nameText) nameText.textContent = displayName;
  if (themeTag) themeTag.textContent = currentMeta.tag;
  if (nametagName) nametagName.textContent = displayName;
  if (nametagMood) nametagMood.textContent = `· ${currentMeta.mood}`;
}

function setStageBg(mood) {
  if (!currentImageId) return;
  const base = currentImageId.replace(/^character-/, '');
  const url = `/images/chat-bg/chat-bg-${base}-${mood}.jpg`;
  rootEl.style.setProperty('--bg-image', `url('${url}')`);
}

// ============================================================
// 호감도 게이지 + 레벨 (--gauge variable + data-level attr)
// ============================================================
function updateStatusBar(affectionScore, level) {
  const fillEl = $(SELECTORS.statusAffectionValue);
  const heartEl = $(SELECTORS.statusLevel);

  // gauge: 한 레벨 안에서의 진행도 (0~99). 백엔드 정책에 따라 조정 가능.
  const gauge = Math.max(0, Math.min(100, (affectionScore ?? 0) % 100));
  if (fillEl) fillEl.style.setProperty('--gauge', `${gauge}%`);

  if (heartEl) heartEl.setAttribute('data-level', String(level ?? 1));
}

function showAffectionDelta(delta, newLevel) {
  const el = $(SELECTORS.affection);
  const deltaEl = $(SELECTORS.affectionDelta);
  const levelUpEl = $(SELECTORS.levelUp);
  if (!el) return;
  el.hidden = false;
  if (deltaEl) {
    if (delta > 0)      deltaEl.textContent = `↑ 호감도 +${delta}`;
    else if (delta < 0) deltaEl.textContent = `↓ 호감도 ${delta}`;
    else                deltaEl.textContent = '→ 호감도 유지';
    deltaEl.style.color = delta > 0
      ? 'var(--color-affection)'
      : delta < 0
      ? '#c9444a'
      : 'var(--base-ink-500)';
  }
  if (levelUpEl) {
    if (newLevel != null && newLevel > 0) {
      levelUpEl.textContent = ` · Lv.${newLevel} 달성!`;
      levelUpEl.hidden = false;
    } else {
      levelUpEl.hidden = true;
    }
  }
}

// ============================================================
// 단일 화자 다이얼로그 + 타자기 스트리밍
//   - showDialogLoading()         : 응답 대기 중 typing-indicator 표시
//   - setDialogPlain(text)        : 즉시 텍스트 세팅 (init 복원/에러 회복용)
//   - setDialogMessage(text, cb)  : 타자기로 글자 차오르듯 표시. 끝나면 cb 호출
//   - skipTypewriter()            : 진행 중인 타자기를 즉시 완료
// ============================================================
const TYPE_BASE_SPEED_MS = 28;     // 글자당 기본 속도
const TYPE_MIN_SPEED_MS  = 14;     // 너무 긴 메시지는 살짝 빠르게
const TYPE_LONG_THRESHOLD = 80;    // 이 길이 넘으면 속도 보정

let typewriterState = {
  el: null,
  fullText: '',
  index: 0,
  timerId: null,
  onComplete: null,
};

function setDialogPlain(text) {
  cancelTypewriter();
  const container = $(SELECTORS.messages);
  if (!container) return;
  container.innerHTML = `<span data-chat-current-message style="opacity: 0; animation: word-fade 360ms var(--ease-soft) forwards;"></span>`;
  const slot = $(SELECTORS.currentMessage);
  if (slot) slot.textContent = text || '대화를 시작해 보세요.';
}

function showDialogLoading(label = '음… 잠깐만요') {
  cancelTypewriter();
  const container = $(SELECTORS.messages);
  if (!container) return;
  container.innerHTML = `
    <span class="dialog__thinking" aria-live="polite">
      <span class="typing-indicator"><span></span><span></span><span></span></span>
      <span class="dialog__thinking__label">${escapeHtml(label)}</span>
    </span>`;
}

function setDialogMessage(aiMessage, onComplete) {
  cancelTypewriter();
  const container = $(SELECTORS.messages);
  if (!container) return;
  const text = aiMessage || '대화를 시작해 보세요.';

  // 글자가 차오를 빈 슬롯으로 시작
  container.innerHTML = `<span data-chat-current-message class="dialog__current-message dialog__current-message--typing" style="opacity: 0; animation: word-fade 240ms var(--ease-soft) forwards;"></span>`;
  const slot = $(SELECTORS.currentMessage);
  if (!slot) return;

  // 길이별 속도 — 짧으면 또박또박, 길면 살짝 빠르게
  const speed = text.length > TYPE_LONG_THRESHOLD
    ? Math.max(TYPE_MIN_SPEED_MS, Math.round(TYPE_BASE_SPEED_MS * (TYPE_LONG_THRESHOLD / text.length)))
    : TYPE_BASE_SPEED_MS;

  typewriterState.el = slot;
  typewriterState.fullText = text;
  typewriterState.index = 0;
  typewriterState.onComplete = typeof onComplete === 'function' ? onComplete : null;

  setSkipAvailable(true);

  typewriterState.timerId = setInterval(() => {
    typewriterState.index += 1;
    typewriterState.el.textContent = typewriterState.fullText.slice(0, typewriterState.index);
    if (typewriterState.index >= typewriterState.fullText.length) {
      finishTypewriter();
    }
  }, speed);
}

function skipTypewriter() {
  if (!typewriterState.timerId) return;
  if (typewriterState.el) {
    typewriterState.el.textContent = typewriterState.fullText;
  }
  finishTypewriter();
}

function finishTypewriter() {
  if (typewriterState.timerId) {
    clearInterval(typewriterState.timerId);
    typewriterState.timerId = null;
  }
  if (typewriterState.el) {
    typewriterState.el.classList.remove('dialog__current-message--typing');
  }
  setSkipAvailable(false);
  const cb = typewriterState.onComplete;
  typewriterState.onComplete = null;
  if (cb) cb();
}

function cancelTypewriter() {
  if (typewriterState.timerId) {
    clearInterval(typewriterState.timerId);
    typewriterState.timerId = null;
  }
  if (typewriterState.el) {
    typewriterState.el.classList.remove('dialog__current-message--typing');
  }
  setSkipAvailable(false);
  typewriterState.el = null;
  typewriterState.onComplete = null;
}

function isTyping() {
  return typewriterState.timerId !== null;
}

function setSkipAvailable(active) {
  const skipBtn = rootEl?.querySelector('[data-chat-toggle-skip]');
  if (!skipBtn) return;
  skipBtn.classList.toggle('dialog__toggle--skip-active', !!active);
}

// ============================================================
// 선택지 모달 — .choice-card 마크업 (cut + voice cue + stagger fade-up)
// ============================================================
function showChoiceModal(aiMessage, choices) {
  const modal = $(SELECTORS.choiceModal);
  const messageEl = $(SELECTORS.choiceMessage);
  const buttonsEl = $(SELECTORS.choiceButtons);
  if (!modal || !buttonsEl) return;

  // 다이얼로그 본문에 질문이 단일 화자로 이미 표시되어 있음 — choiceMessage 는 보조용 (hidden 유지)
  if (messageEl) messageEl.textContent = aiMessage || '';

  const items = (choices || []).map((c) => (typeof c === 'string' ? { text: c } : c));
  const useGrid = items.length === 4;
  const useTwo = items.length === 2;
  buttonsEl.className = 'choice-modal__list' +
    (useGrid ? ' choice-modal__list--grid' : useTwo ? ' choice-modal__list--two' : '');
  buttonsEl.innerHTML = items.map(renderChoiceCard).join('');

  modal.hidden = false;
  setInputEnabled(false);

  buttonsEl.querySelectorAll('.choice-card').forEach((btn) => {
    btn.addEventListener('click', () => {
      btn.classList.add('choice-card--chosen');
      buttonsEl.querySelectorAll('.choice-card').forEach((other) => {
        if (other !== btn) other.classList.add('choice-card--dismiss');
      });
      const text = btn.dataset.choiceText || btn.textContent.trim();
      // chosen 애니메이션 후 전송
      setTimeout(() => sendMessage(text), 280);
    });
  });
}

function renderChoiceCard(item) {
  const text = escapeHtml(item.text);
  const voiceCue = voiceMode
    ? '<span class="choice-card__voice-cue" aria-label="이 카드 읽어 말해도 인식됩니다">🎤</span>'
    : '';
  return `
    <button type="button" class="choice-card" data-choice-text="${escapeAttr(item.text)}">
      <span class="choice-card__cut" aria-hidden="true"></span>
      <span class="choice-card__text">${text}</span>
      ${voiceCue}
    </button>`;
}

function hideChoiceModal() {
  const modal = $(SELECTORS.choiceModal);
  if (modal) modal.hidden = true;
}

// ============================================================
// 메시지 전송
// ============================================================
function setLoading(loading) {
  const icon = $(SELECTORS.sendIcon);
  const loadingEl = $(SELECTORS.sendLoading);
  if (icon) icon.hidden = loading;
  if (loadingEl) loadingEl.hidden = !loading;
  const sendBtn = $(SELECTORS.sendBtn);
  if (sendBtn) sendBtn.disabled = loading;
  if (rootEl) rootEl.classList.toggle('chat-sending', loading);
}

function setInputEnabled(enabled) {
  const input = $(SELECTORS.input);
  if (input) input.disabled = !enabled;
  const sendBtn = $(SELECTORS.sendBtn);
  if (sendBtn) sendBtn.disabled = !enabled;
}

async function sendMessage(text) {
  if (!text || !soulmateId) return;
  setLoading(true);
  setInputEnabled(false);
  hideChoiceModal();
  // 응답 대기 폴백 — 다이얼로그 본문에 thinking 인디케이터 (이전 메시지는 가려짐)
  showDialogLoading('음… 잠깐만요');

  // Day 7 Step 9 — 셀카 요청 키워드 감지 시 *셀카 로딩 풍선* 을 다이얼로그 위쪽에 즉시 마운트.
  // 백엔드가 imageUrl null/non-null 로 분기해주므로 응답 도착 후 final 처리.
  const isSelcaReq = /셀카|셀피|사진|selfie|selca/i.test(text);
  if (isSelcaReq) mountSelfie({ loading: true });

  try {
    const res = await postChat(soulmateId, text);
    const delta =
      res.affectionScore != null && previousAffectionScore != null
        ? res.affectionScore - previousAffectionScore
        : 0;
    const levelUp =
      res.level != null && previousLevel != null && res.level > previousLevel
        ? res.level
        : null;
    previousAffectionScore = res.affectionScore ?? previousAffectionScore;
    previousLevel = res.level ?? previousLevel;

    updateStatusBar(res.affectionScore, res.level);
    if (delta > 0) setStageBg('happy');
    else if (delta < 0) setStageBg('sad');
    else setStageBg('neutral');

    showAffectionDelta(delta, levelUp);

    // Day 7 Step 9 — 셀카 응답 imageUrl 분기. 도착했으면 폴라로이드, 아니면 풍선 제거.
    // (가드 한도 초과로 fallback 텍스트만 도착한 경우 imageUrl 이 null 이라 자연스럽게 풍선이 사라짐.)
    if (res.imageUrl) {
      mountSelfie({ loading: false, imageUrl: res.imageUrl });
    } else if (isSelcaReq) {
      // 셀카 요청이었는데 imageUrl 이 null 인 경우 (가드 우회) — 로딩 풍선 제거.
      removeSelfie();
    }

    const hasChoices = !!(res.choices && res.choices.length > 0);

    // 타자기로 텍스트가 차오른 뒤 → 완료 콜백에서 choice 모달 솟아오름 / 입력 재활성
    setLoading(false);
    setDialogMessage(res.aiMessage, () => {
      if (hasChoices) {
        showChoiceModal(res.aiMessage, res.choices);
      } else {
        setInputEnabled(true);
      }
    });

    // VOICE 토글 ON 이면 AI 메시지를 TTS 로 합성해 재생 (타자기와 병렬 진행)
    if (voiceMode && res.aiMessage) {
      playReplyAudio(res.aiMessage, pickVoice(currentImageId)).catch((e) => {
        console.warn('[voice] TTS playback skipped:', e?.message || e);
      });
    }
  } catch (err) {
    cancelTypewriter();
    setDialogPlain('전송이 실패했어요. 다시 시도해 보세요.');
    setLoading(false);
    setInputEnabled(true);
    alert(err.message || '전송에 실패했어요');
  }
}

// ============================================================
// 햄버거 메뉴
// ============================================================
function toggleMenu() {
  const hamburger = $(SELECTORS.hamburger);
  const dropdown = $(SELECTORS.dropdown);
  if (!hamburger || !dropdown) return;
  const isOpen = dropdown.getAttribute('data-open') === 'true';
  if (isOpen) {
    dropdown.setAttribute('data-open', 'false');
    dropdown.hidden = true;
    hamburger.setAttribute('aria-expanded', 'false');
  } else {
    dropdown.removeAttribute('hidden');
    dropdown.setAttribute('data-open', 'true');
    hamburger.setAttribute('aria-expanded', 'true');
  }
}

function closeMenu() {
  const dropdown = $(SELECTORS.dropdown);
  const hamburger = $(SELECTORS.hamburger);
  if (dropdown) {
    dropdown.setAttribute('data-open', 'false');
    dropdown.hidden = true;
  }
  if (hamburger) hamburger.setAttribute('aria-expanded', 'false');
}

// ============================================================
// LOG 모달 (미연시 백로그) — log-msg / log-user / log-event
// ============================================================
function formatBubbleTime(isoString) {
  if (!isoString) return '';
  const d = new Date(isoString);
  const now = new Date();
  const isToday = d.toDateString() === now.toDateString();
  if (isToday) {
    return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false });
  }
  return (
    d.toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' }) + ' ' +
    d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false })
  );
}

function renderHistoryEntry(log) {
  const isUser = log.speaker === 'USER';
  const time = escapeHtml(formatBubbleTime(log.createdAt));
  const text = escapeHtml(log.message || '');
  if (isUser) {
    return `
      <div class="log-user">
        <span>${text}</span>
        <span class="log-user__time">${time}</span>
      </div>`;
  }
  const charName = escapeHtml(storedProfile?.name || '소울메이트');
  return `
    <div class="log-msg log-msg--ai">
      <div class="log-msg__face" aria-hidden="true"></div>
      <div class="log-msg__body">
        <div class="log-msg__head">
          <span class="log-msg__name">${charName}</span>
          <span class="log-msg__time">${time}</span>
        </div>
        <div class="log-msg__text">${text}</div>
      </div>
    </div>`;
}

function showHistoryLoad(show) {
  const el = $(SELECTORS.historyLoad);
  if (el) el.hidden = !show;
}

async function loadHistoryPage(prepend = false) {
  if (!soulmateId || historyLoading) return;
  historyLoading = true;
  showHistoryLoad(true);
  try {
    const { content, hasNext } = await getChatLogs(soulmateId, historyNextPage, HISTORY_PAGE_SIZE);
    historyHasMore = hasNext;
    historyNextPage += 1;
    const listEl = $(SELECTORS.historyList);
    if (!listEl) return;
    const reversed = [...content].reverse();
    const html = reversed.map(renderHistoryEntry).join('');
    if (prepend) {
      const prevScrollHeight = historyScrollEl?.scrollHeight ?? 0;
      const prevScrollTop = historyScrollEl?.scrollTop ?? 0;
      listEl.insertAdjacentHTML('afterbegin', html);
      requestAnimationFrame(() => {
        if (historyScrollEl)
          historyScrollEl.scrollTop = historyScrollEl.scrollHeight - prevScrollHeight + prevScrollTop;
      });
    } else {
      listEl.innerHTML = html;
      requestAnimationFrame(() => {
        if (historyScrollEl) historyScrollEl.scrollTop = historyScrollEl.scrollHeight;
      });
    }
  } catch (e) {
    const listEl = $(SELECTORS.historyList);
    if (listEl) {
      listEl.insertAdjacentHTML(
        'afterbegin',
        `<div class="log-event">${escapeHtml(e.message || '불러오기 실패')}</div>`,
      );
    }
  } finally {
    historyLoading = false;
    showHistoryLoad(false);
  }
}

function onHistoryScroll() {
  if (!historyScrollEl || !historyHasMore || historyLoading) return;
  if (historyScrollEl.scrollTop < 120) loadHistoryPage(true);
}

function openHistoryModal() {
  const modal = $(SELECTORS.historyModal);
  if (!modal) return;
  historyNextPage = 0;
  historyHasMore = true;
  modal.hidden = false;
  loadHistoryPage(false);
  // log-list 가 실제 scroll container (log-panel 은 overflow:hidden)
  historyScrollEl = $(SELECTORS.historyList);
  if (historyScrollEl) {
    historyScrollEl.addEventListener('scroll', onHistoryScroll, { passive: true });
  }
}

function closeHistoryModal() {
  const modal = $(SELECTORS.historyModal);
  if (historyScrollEl) historyScrollEl.removeEventListener('scroll', onHistoryScroll);
  if (modal) modal.hidden = true;
}

// ============================================================
// 음성 토글 + 마이크 권한 (default OFF, hooks only — Day 9 에서 활성화)
// ============================================================
function getMicPermission() {
  return localStorage.getItem(MIC_PERM_KEY); // 'granted' | 'deferred' | null
}
function setMicPermission(v) {
  localStorage.setItem(MIC_PERM_KEY, v);
}

function showMicPermModal() {
  const modal = $(SELECTORS.micPerm);
  if (modal) modal.hidden = false;
}
function hideMicPermModal() {
  const modal = $(SELECTORS.micPerm);
  if (modal) modal.hidden = true;
}

function setVoiceMode(on) {
  voiceMode = !!on;
  const toggleBtn = $(SELECTORS.voiceToggle);
  if (!toggleBtn) return;
  toggleBtn.classList.toggle('dialog__toggle--on', voiceMode);
  toggleBtn.setAttribute('aria-pressed', voiceMode ? 'true' : 'false');
}

function toggleVoiceMode() {
  if (!voiceMode) {
    if (getMicPermission() === null) {
      // 첫 1회 — 마이크 권한 모달 노출, 결과로 voiceMode 결정
      showMicPermModal();
      return;
    }
    if (getMicPermission() === 'deferred') {
      // 이전에 거부한 사용자 — 다시 모달 한번 더 보여줌
      showMicPermModal();
      return;
    }
    setVoiceMode(true);
  } else {
    setVoiceMode(false);
  }
}

async function onMicPermAllow() {
  hideMicPermModal();
  // 실제 OS 권한 다이얼로그 — 사용자가 거부하면 catch
  try {
    if (!mediaStream) {
      mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    }
    setMicPermission('granted');
    setVoiceMode(true);
  } catch (e) {
    console.warn('[voice] getUserMedia denied:', e?.message || e);
    setMicPermission('deferred');
    setVoiceMode(false);
    alert('마이크 권한이 거부되어 음성 모드를 켤 수 없어요. 브라우저 설정에서 허용을 다시 체크해 주세요.');
  }
}

function onMicPermDefer() {
  setMicPermission('deferred');
  hideMicPermModal();
  setVoiceMode(false);
}

function setVoiceTranscript(text) {
  const el = $(SELECTORS.voiceTranscript);
  if (!el) return;
  if (text) {
    el.textContent = text;
    el.hidden = false;
  } else {
    el.textContent = '';
    el.hidden = true;
  }
}

async function onMicRecord() {
  // 토글: 녹음 중이면 stop (이후 onstop 콜백이 STT 호출까지 처리)
  if (isRecording) {
    stopRecording();
    return;
  }

  // 시작 시퀀스가 이미 진행 중이면 중복 트리거 방지
  if (recordStartTimer) return;

  // 권한 보유 — 없으면 즉시 요청 (브라우저 OS 다이얼로그 노출). 마이크 버튼 클릭 자체가
  // user gesture 라 getUserMedia 를 직접 불러도 자동재생 정책 위반이 아니다.
  if (!mediaStream) {
    try {
      mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
      setMicPermission('granted');
    } catch (e) {
      console.warn('[voice] getUserMedia denied:', e?.message || e);
      setMicPermission('deferred');
      alert('마이크 권한이 거부됐어요. 브라우저 주소창 옆 자물쇠 → 마이크 허용으로 켜 주세요.');
      return;
    }
  }

  // 음성으로 시작했으니 응답도 음성으로 들려준다 — VOICE 토글도 함께 ON.
  if (!voiceMode) setVoiceMode(true);

  // STT 시작 시퀀스 — BGM 페이드아웃 + 띠링 효과음 → 약 380ms 후 녹음 시작
  pauseBgmForRecording();
  const chimeMs = playStartChime();
  recordStartTimer = setTimeout(() => {
    recordStartTimer = null;
    startRecording();
  }, Math.max(380, chimeMs - 20));
}

// ----- 녹음 시 input-row 변형: input + send 숨김, voice-time + voice-wave + cancel 표시 -----
function enterVoiceInputMode() {
  const row = $(SELECTORS.inputRow);
  if (!row) return;
  row.classList.add('dialog__input-row--voice');

  // 기존 input + send 숨김
  const input = $(SELECTORS.input);
  const sendBtn = $(SELECTORS.sendBtn);
  if (input) input.hidden = true;
  if (sendBtn) sendBtn.hidden = true;

  // voice 변형 요소 — 한 번만 만들어서 재사용. 마이크 버튼 앞에 삽입.
  const mic = $(SELECTORS.voiceRecord);
  if (!mic) return;

  if (!row.querySelector('[data-chat-voice-time]')) {
    const time = document.createElement('span');
    time.className = 'voice-time';
    time.setAttribute('data-chat-voice-time', '');
    time.innerHTML = '<span class="voice-time__dot" aria-hidden="true"></span><span data-chat-voice-time-text>00:00</span>';
    row.insertBefore(time, mic);
  }
  if (!row.querySelector('.voice-wave')) {
    const wave = document.createElement('div');
    wave.className = 'voice-wave';
    wave.setAttribute('aria-label', '녹음 중');
    // 12개 막대 — wave-bounce 애니메이션
    wave.innerHTML = Array.from({ length: 12 }).map(() => '<span></span>').join('');
    row.insertBefore(wave, mic);
  }
  if (!row.querySelector(SELECTORS.voiceCancel)) {
    const cancel = document.createElement('button');
    cancel.type = 'button';
    cancel.className = 'dialog__cancel';
    cancel.setAttribute('aria-label', '녹음 취소');
    cancel.setAttribute('data-chat-voice-cancel', '');
    cancel.innerHTML = '<svg viewBox="0 0 14 14" width="11" height="11" fill="none"><path d="M3 3l8 8M11 3L3 11" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" /></svg>';
    cancel.addEventListener('click', cancelRecording);
    row.insertBefore(cancel, mic);
  }
}

function exitVoiceInputMode() {
  const row = $(SELECTORS.inputRow);
  if (!row) return;
  row.classList.remove('dialog__input-row--voice');

  // voice 요소 제거
  row.querySelectorAll('[data-chat-voice-time], .voice-wave, [data-chat-voice-cancel]').forEach((el) => el.remove());

  // input + send 복원
  const input = $(SELECTORS.input);
  const sendBtn = $(SELECTORS.sendBtn);
  if (input) input.hidden = false;
  if (sendBtn) sendBtn.hidden = false;
}

function startRecordTimer() {
  recordStartedAt = Date.now();
  const tick = () => {
    const elapsed = Math.floor((Date.now() - recordStartedAt) / 1000);
    const mm = String(Math.floor(elapsed / 60)).padStart(2, '0');
    const ss = String(elapsed % 60).padStart(2, '0');
    const el = rootEl.querySelector('[data-chat-voice-time-text]');
    if (el) el.textContent = `${mm}:${ss}`;
  };
  tick();
  recordTimerId = setInterval(tick, 250);
}
function stopRecordTimer() {
  if (recordTimerId) {
    clearInterval(recordTimerId);
    recordTimerId = null;
  }
}

function startRecording() {
  // MediaRecorder 의 webm/opus 컨테이너 — Whisper 가 받아들이는 화이트리스트의 webm.
  let mr;
  try {
    mr = new MediaRecorder(mediaStream, { mimeType: 'audio/webm' });
  } catch (e) {
    // 일부 브라우저(Safari 일부)는 webm 미지원 — mime 미지정으로 폴백
    mr = new MediaRecorder(mediaStream);
  }
  recordedChunks = [];
  recordCanceled = false;
  mr.addEventListener('dataavailable', (ev) => {
    if (ev.data && ev.data.size > 0) recordedChunks.push(ev.data);
  });
  mr.addEventListener('stop', onRecordingStop);
  mr.start();
  mediaRecorder = mr;
  isRecording = true;
  updateRecordButton();
  enterVoiceInputMode();
  startRecordTimer();
  setVoiceTranscript('');
}

function stopRecording() {
  if (!mediaRecorder || !isRecording) return;
  isRecording = false;
  updateRecordButton();
  stopRecordTimer();
  exitVoiceInputMode();
  // BGM 재개 — STT/AI 응답 동안 자연스럽게 깔리고, TTS 가 오면 ducking 으로 다시 줄어든다
  resumeBgmAfterRecording();
  showTranscriptHint('🎧 음성 인식 중…');
  try {
    mediaRecorder.stop(); // → onstop → onRecordingStop
  } catch (e) {
    console.warn('[voice] stop failed:', e);
    setVoiceTranscript('');
  }
}

function cancelRecording() {
  if (!mediaRecorder || !isRecording) return;
  recordCanceled = true;
  isRecording = false;
  updateRecordButton();
  stopRecordTimer();
  exitVoiceInputMode();
  resumeBgmAfterRecording();
  setVoiceTranscript('');
  try { mediaRecorder.stop(); } catch (_) { /* noop */ }
}

async function onRecordingStop() {
  const chunks = recordedChunks;
  recordedChunks = [];

  if (recordCanceled) {
    recordCanceled = false;
    return;
  }
  if (!chunks.length) {
    setVoiceTranscript('');
    return;
  }
  const blob = new Blob(chunks, { type: mediaRecorder?.mimeType || 'audio/webm' });

  // 짧은 발화는 Whisper 가 hallucinate 하는 경향 → 4KB 미만(약 0.5s) 은 컷
  if (blob.size < 4000) {
    showTranscriptHint('🎙 너무 짧게 녹음됐어요. 한 문장 이상 말해 주세요.');
    setTimeout(() => setVoiceTranscript(''), 1800);
    return;
  }

  try {
    const text = await transcribeAudio(blob, 'recording.webm');
    if (!text || !text.trim()) {
      showTranscriptHint('🎙 인식된 말이 없어요. 다시 한 번 말해 주세요.');
      setTimeout(() => setVoiceTranscript(''), 1800);
      return;
    }
    // 디자인 명세 — partial transcript 우상단에 단어별 .word fade-in
    showTranscriptWords(text);
    await sendMessage(text);
  } catch (e) {
    setVoiceTranscript('');
    alert(e?.message || '음성 인식에 실패했어요');
  }
}

function updateRecordButton() {
  const btn = $(SELECTORS.voiceRecord);
  if (!btn) return;
  btn.classList.toggle('dialog__mic--rec', isRecording);
  btn.setAttribute('aria-pressed', isRecording ? 'true' : 'false');
  btn.setAttribute('aria-label', isRecording ? '녹음 종료' : '음성 입력');
}

// ----- partial transcript: 단어별 .word span + word-fade 애니메이션 (디자인 명세 2-2) -----
function showTranscriptWords(text) {
  const el = $(SELECTORS.voiceTranscript);
  if (!el) return;
  const words = (text || '').split(/\s+/).filter(Boolean);
  el.classList.add('dialog__transcript--final');
  el.innerHTML = words
    .map((w, i) => `<span class="word" style="animation-delay: ${i * 90}ms">${escapeHtml(w)} </span>`)
    .join('');
  el.hidden = false;
  // 메시지가 화면에 박히고 잠시 뒤 자연스럽게 페이드아웃 (디자인의 "transcript-rise" 흐름)
  clearTimeout(showTranscriptWords._t);
  showTranscriptWords._t = setTimeout(() => setVoiceTranscript(''), 3200);
}

function showTranscriptHint(text) {
  const el = $(SELECTORS.voiceTranscript);
  if (!el) return;
  el.classList.remove('dialog__transcript--final');
  el.textContent = text;
  el.hidden = false;
}

// ----- TTS 생성 대기 — 본문 blur + tts-pending 표시 (디자인 명세 2-4) -----
function setTtsAwaiting(on) {
  const body = $(SELECTORS.messages);
  const frame = $(SELECTORS.dialogFrame);
  if (!body || !frame) return;

  body.classList.toggle('dialog__body--awaiting', !!on);

  let pending = frame.querySelector('[data-tts-pending]');
  if (on) {
    if (!pending) {
      pending = document.createElement('div');
      pending.className = 'tts-pending';
      pending.setAttribute('data-tts-pending', '');
      pending.innerHTML = '<span class="typing-indicator"><span></span><span></span><span></span></span>음성 생성 중';
      // 본문 바로 아래에 박는다
      body.insertAdjacentElement('afterend', pending);
    }
  } else if (pending) {
    pending.remove();
  }
}

// ----- AI 음성 재생 중 — voice-ring + voice-notes + dialog__body--vplay (디자인 명세 2-3) -----
function setTtsPlaying(on) {
  const frame = $(SELECTORS.dialogFrame);
  const body = $(SELECTORS.messages);
  const portrait = $(SELECTORS.portrait);
  if (!frame || !body || !portrait) return;

  // BGM ducking — TTS 재생 중에는 BGM 볼륨을 줄여 캐릭터 음성을 부각
  setTtsActive(!!on);

  body.classList.toggle('dialog__body--vplay', !!on);
  portrait.classList.toggle('dialog__portrait--playing', !!on);

  let ring = frame.querySelector('.voice-ring');
  let notes = frame.querySelector('.voice-notes');
  if (on) {
    if (!ring) {
      ring = document.createElement('div');
      ring.className = 'voice-ring';
      ring.setAttribute('aria-hidden', 'true');
      ring.innerHTML = '<div class="voice-ring__inner"></div>';
      portrait.insertAdjacentElement('afterend', ring);
    }
    if (!notes) {
      notes = document.createElement('div');
      notes.className = 'voice-notes';
      notes.setAttribute('aria-hidden', 'true');
      notes.innerHTML = '<span>♪</span><span>♫</span><span>♪</span><span>♬</span>';
      portrait.insertAdjacentElement('afterend', notes);
    }
  } else {
    if (ring) ring.remove();
    if (notes) notes.remove();
  }
}

async function playReplyAudio(text, voice) {
  // 이미 재생 중인 응답이 있으면 멈추고 교체
  if (currentReplyAudio) {
    try { currentReplyAudio.pause(); } catch (_) { /* noop */ }
    currentReplyAudio = null;
    setTtsPlaying(false);
  }

  // TTS 생성 대기 — blur placeholder
  setTtsAwaiting(true);
  let result;
  try {
    result = await synthesizeSpeech(text, voice);
  } finally {
    setTtsAwaiting(false);
  }

  const url = URL.createObjectURL(result.blob);
  const audio = new Audio(url);
  const cleanup = () => {
    URL.revokeObjectURL(url);
    if (currentReplyAudio === audio) {
      currentReplyAudio = null;
      setTtsPlaying(false);
    }
  };
  audio.addEventListener('ended', cleanup);
  audio.addEventListener('error', cleanup);
  currentReplyAudio = audio;
  setTtsPlaying(true);
  // play() 는 user-gesture chain 안에서 호출되어야 reject 안 됨 — onMicRecord 핸들러에서 이어진 호출이라 통과
  await audio.play();
}

// ============================================================
// 보정⑥ — 다이얼로그 박스 높이에 따라 --dialog-stack-bottom 동적 갱신
//   다이얼로그 콘텐츠(메시지/입력창/토글) 높이가 변하면 선택지 모달 위치도 재계산
// ============================================================
function setupDialogStackObserver() {
  const dialogFrame = $(SELECTORS.dialogFrame);
  if (!dialogFrame) return;
  if (typeof ResizeObserver === 'undefined') return;
  const ro = new ResizeObserver((entries) => {
    for (const entry of entries) {
      const h = entry.contentRect.height;
      // 다이얼로그 박스 위 공중에 16px 여유 + dialog 컨테이너 bottom: 18px = 약 34px
      rootEl.style.setProperty('--dialog-stack-bottom', `${h + 34}px`);
    }
  });
  ro.observe(dialogFrame);
}

// ============================================================
// 페이지 진입 시 — 가장 최근 AI 발화를 다이얼로그에 복원
//   getChatLogs 의 page 0 은 DESC(최신 먼저). USER 가 아닌 첫 항목이 마지막 AI 발화.
//   기록이 없거나 모두 USER 발화뿐이면 기본 안내 문구를 그대로 둔다.
// ============================================================
async function restoreLastAssistantMessage() {
  if (!soulmateId) return;
  try {
    const { content } = await getChatLogs(soulmateId, 0, 10);
    if (!content || !content.length) return;
    const lastAi = content.find((log) => log.speaker !== 'USER');
    if (lastAi && lastAi.message) {
      // 진입 복원은 즉시 표시 — 타자기는 신규 AI 응답에서만 의미 있다
      setDialogPlain(lastAi.message);
    }
  } catch (e) {
    // 복원 실패는 critical 하지 않다 — 기본 문구로 남겨둔다
    console.warn('[chat] failed to restore last message:', e?.message || e);
  }
}

// ============================================================
// 초기화
// ============================================================
function init() {
  rootEl = document.querySelector(SELECTORS.root);
  if (!rootEl) return;
  const id = rootEl.getAttribute('data-soulmate-id');
  soulmateId = id ? parseInt(id, 10) : null;
  if (!soulmateId) {
    window.location.replace(HOME_URL);
    return;
  }

  getSoulmate(soulmateId)
    .then((profile) => {
      storedProfile = profile;
      previousAffectionScore = profile.affectionScore ?? 0;
      previousLevel = profile.level ?? 1;
      applyCharacterTheme(profile);
      updateStatusBar(previousAffectionScore, previousLevel);
      requestAnimationFrame(() => rootEl.classList.add('chat-entered'));
      // 직전 대화의 마지막 AI 발화 복원 — 진입 시 흐름이 끊기지 않도록
      restoreLastAssistantMessage();
    })
    .catch(() => {
      window.location.replace(HOME_URL);
    });

  // 입력 이벤트
  const input = $(SELECTORS.input);
  const sendBtn = $(SELECTORS.sendBtn);
  if (input) {
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        const v = input.value.trim();
        if (v) {
          sendMessage(v);
          input.value = '';
        }
      }
    });
  }
  if (sendBtn) {
    sendBtn.addEventListener('click', () => {
      const v = input?.value?.trim();
      if (v) {
        sendMessage(v);
        if (input) input.value = '';
      }
    });
  }

  // 햄버거 메뉴
  const hamburger = $(SELECTORS.hamburger);
  const menuHome = $(SELECTORS.menuHome);
  if (hamburger) hamburger.addEventListener('click', toggleMenu);
  if (menuHome) menuHome.addEventListener('click', closeMenu);

  // 대화 기록 진입점은 두 곳: 햄버거 드롭다운의 "대화 기록" + dialog__toggles 의 LOG.
  // 둘 다 data-chat-menu-history 가 박혀 있으므로 한꺼번에 잡는다.
  $$('[data-chat-menu-history]').forEach((btn) => {
    btn.addEventListener('click', () => {
      closeMenu();
      openHistoryModal();
    });
  });

  // LOG 모달 닫기
  const historyBackdrop = $(SELECTORS.historyBackdrop);
  const historyClose = $(SELECTORS.historyClose);
  if (historyBackdrop) historyBackdrop.addEventListener('click', closeHistoryModal);
  if (historyClose) historyClose.addEventListener('click', closeHistoryModal);

  // 음성 토글 + 마이크 권한
  const voiceToggle = $(SELECTORS.voiceToggle);
  const voiceRecord = $(SELECTORS.voiceRecord);
  const micPermAllow = $(SELECTORS.micPermAllow);
  const micPermDefer = $(SELECTORS.micPermDefer);
  if (voiceToggle) voiceToggle.addEventListener('click', toggleVoiceMode);
  if (voiceRecord) voiceRecord.addEventListener('click', onMicRecord);
  if (micPermAllow) micPermAllow.addEventListener('click', onMicPermAllow);
  if (micPermDefer) micPermDefer.addEventListener('click', onMicPermDefer);

  // SKIP 토글 — 진행 중인 타자기를 즉시 완료 (이후 onComplete 가 choice 노출/입력 활성)
  const skipBtn = rootEl.querySelector('[data-chat-toggle-skip]');
  if (skipBtn) {
    skipBtn.addEventListener('click', () => {
      if (isTyping()) skipTypewriter();
    });
  }

  // 보정⑥
  setupDialogStackObserver();

  // BGM — 자동 재생 시도 + mute 토글 + TTS ducking 훅 연결
  initBgm();

  // Day 7 Step 9 — 셀카 빠른 버튼 + 라이트박스 핸들러 박기
  bindSelfieFeatures();
}

// ============================================================
// Day 7 Step 9 — 셀카 응답 풍선 + 빠른 버튼 + 라이트박스
// (Claude Design handoff 통합)
// ============================================================

/**
 * 셀카 풍선 마운트. imageUrl 이 있으면 폴라로이드, 없으면 로딩 스켈레톤.
 * .dialog 컨테이너 안 .dialog__frame 형제 자리의 [data-selfie] 를 갱신한다.
 */
function mountSelfie({ loading = false, imageUrl = null, caption = '' }) {
  const root = rootEl || document.querySelector('[data-app-root]');
  if (!root) return;
  const wrap = root.querySelector('[data-selfie]');
  if (!wrap) return;

  wrap.hidden = false;
  wrap.classList.toggle('selfie--loading', !!loading);
  if (imageUrl) {
    wrap.style.setProperty('--selfie-image', `url('${imageUrl}')`);
  } else if (loading) {
    wrap.style.removeProperty('--selfie-image');
  }
  const captionEl = wrap.querySelector('[data-selfie-caption]');
  if (captionEl) {
    captionEl.innerHTML = loading
      ? '<span class="blink">📸 캐릭터가 셀카를 찍는 중…</span>'
      : (caption || '');
  }
}

function removeSelfie() {
  const root = rootEl || document.querySelector('[data-app-root]');
  if (!root) return;
  const wrap = root.querySelector('[data-selfie]');
  if (!wrap) return;
  wrap.hidden = true;
  wrap.classList.remove('selfie--loading');
  wrap.style.removeProperty('--selfie-image');
}

/** 셀카 빠른 버튼 + 라이트박스 핸들러를 *한 번만* 바인드. */
function bindSelfieFeatures() {
  const root = rootEl || document.querySelector('[data-app-root]');
  if (!root) return;

  // 빠른 버튼 — 클릭 시 입력란에 자동 채움 + 전송
  const quickSelfie = root.querySelector('[data-quick-selfie]');
  const quickMood = root.querySelector('[data-quick-mood]');
  const chatInput = root.querySelector('[data-chat-input]');

  if (quickSelfie && chatInput) {
    quickSelfie.addEventListener('click', () => {
      chatInput.value = '셀카 보내줘';
      chatInput.focus();
      sendMessage(chatInput.value);
      chatInput.value = '';
    });
  }
  if (quickMood && chatInput) {
    quickMood.addEventListener('click', () => {
      chatInput.value = '오늘 어땠어?';
      chatInput.focus();
      sendMessage(chatInput.value);
      chatInput.value = '';
    });
  }

  // 라이트박스 — 사진 클릭 시 풀스크린, ESC/배경/X 버튼으로 닫힘
  const selfieFrame = root.querySelector('[data-selfie-frame]');
  const lightbox = root.querySelector('[data-lightbox]');
  const lightboxImg = root.querySelector('[data-lightbox-img]');
  const lightboxClose = root.querySelector('[data-lightbox-close]');

  if (selfieFrame && lightbox && lightboxImg) {
    selfieFrame.addEventListener('click', () => {
      const wrap = root.querySelector('[data-selfie]');
      if (!wrap || wrap.classList.contains('selfie--loading')) return;
      const url = wrap.style.getPropertyValue('--selfie-image');
      if (!url) return;
      lightboxImg.style.backgroundImage = url;
      lightbox.dataset.open = 'true';
    });
  }
  if (lightboxClose && lightbox) {
    lightboxClose.addEventListener('click', () => {
      lightbox.dataset.open = 'false';
    });
  }
  if (lightbox) {
    lightbox.addEventListener('click', (e) => {
      if (e.target === lightbox) lightbox.dataset.open = 'false';
    });
  }
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && lightbox && lightbox.dataset.open === 'true') {
      lightbox.dataset.open = 'false';
    }
  });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
