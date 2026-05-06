/**
 * 채팅 화면 — 미연시 정석 A 패러다임 (단일 화자 다이얼로그 + LOG 백로그 + VOICE 훅)
 *
 *  - 단일 화자: 다이얼로그 박스에 마지막 한 마디만, 이전 대화는 LOG 모달
 *  - 캐릭터 테마(bright/warm/calm/cheerful) 는 root 의 CSS variable 로 전파
 *  - 음성 입력은 default OFF — VOICE 토글 ON 시 첫 1회 마이크 권한 모달.
 *    실제 STT/TTS 활성화는 Day 9 에서 (이 파일은 hooks only).
 */
import { getSoulmate, postChat, getChatLogs } from '../../api.js';

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
// 단일 화자 다이얼로그 (마지막 한 마디만 표시)
// ============================================================
function setDialogMessage(aiMessage) {
  const container = $(SELECTORS.messages);
  if (!container) return;
  const text = aiMessage || '대화를 시작해 보세요.';
  // 노드 교체 → animations.css 의 word-fade 재실행
  container.innerHTML = `<span data-chat-current-message style="opacity: 0; animation: word-fade 360ms var(--ease-soft) forwards;"></span>`;
  const slot = $(SELECTORS.currentMessage);
  if (slot) slot.textContent = text;
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
    setDialogMessage(res.aiMessage);
    showAffectionDelta(delta, levelUp);

    if (res.choices && res.choices.length > 0) {
      showChoiceModal(res.aiMessage, res.choices);
    } else {
      setInputEnabled(true);
    }
  } catch (err) {
    alert(err.message || '전송에 실패했어요');
    setInputEnabled(true);
  } finally {
    setLoading(false);
    const modal = $(SELECTORS.choiceModal);
    if (modal && !modal.hidden) {
      setInputEnabled(false);
    }
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

function onMicPermAllow() {
  setMicPermission('granted');
  hideMicPermModal();
  setVoiceMode(true);
  // Day 9 에서 실제 navigator.mediaDevices.getUserMedia({ audio: true }) 호출 예정
}

function onMicPermDefer() {
  setMicPermission('deferred');
  hideMicPermModal();
  setVoiceMode(false);
}

function onMicRecord() {
  if (!voiceMode) return;
  // Day 9 에서 MediaRecorder + STT 활성화 — 일단 placeholder
  console.info('[voice] mic record placeholder — Day 9 에서 활성화 예정');
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

  // 보정⑥
  setupDialogStackObserver();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
