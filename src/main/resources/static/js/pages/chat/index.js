/**
 * 채팅 화면 — 미연시 스타일, 전송/로딩/다이얼로그/호감도/선택지 모달, 햄버거 메뉴, 대화 기록 모달
 */
import { getSoulmate, postChat, getChatLogs } from '../../api.js';

const SELECTORS = {
  root: '[data-app-root]',
  bg: '[data-chat-bg]',
  statusAffection: '[data-chat-status-affection]',
  statusAffectionValue: '[data-chat-status-affection-value]',
  statusLevel: '[data-chat-status-level]',
  hamburger: '[data-chat-hamburger]',
  dropdown: '[data-chat-dropdown]',
  menuHome: '[data-chat-menu-home]',
  menuHistory: '[data-chat-menu-history]',
  messages: '[data-chat-messages]',
  affection: '[data-chat-affection]',
  affectionDelta: '[data-chat-affection-delta]',
  levelUp: '[data-chat-level-up]',
  input: '[data-chat-input]',
  sendBtn: '[data-chat-send]',
  sendIcon: '[data-chat-send-icon]',
  sendLoading: '[data-chat-send-loading]',
  choiceModal: '[data-chat-choice-modal]',
  choiceMessage: '[data-chat-choice-message]',
  choiceButtons: '[data-chat-choice-buttons]',
  historyModal: '[data-chat-history-modal]',
  historyBackdrop: '[data-chat-history-backdrop]',
  historyClose: '[data-chat-history-close]',
  historyScroll: '[data-chat-history-scroll]',
  historyList: '[data-chat-history-list]',
  historyLoad: '[data-chat-history-load]',
};

/** characterImageId → chat-bg 파일 접두사 */
function getChatBgBase(characterImageId) {
  if (!characterImageId || typeof characterImageId !== 'string') return null;
  const base = characterImageId.replace(/^character-/, '');
  const allowed = [
    'female-bright',
    'female-warm',
    'male-calm',
    'male-cheerful',
  ];
  return allowed.includes(base) ? base : null;
}
function getChatBgUrl(base, mood) {
  if (!base) return null;
  return `/images/chat-bg/chat-bg-${base}-${mood}.jpg`;
}
/** characterImageId → 캐릭터 얼굴 이미지 URL (히스토리 AI 버블 아바타용) */
function getCharacterFaceUrl(characterImageId) {
  if (!characterImageId || typeof characterImageId !== 'string') return null;
  return `/images/characters/${characterImageId}-face.jpg`;
}

let soulmateId;
let previousAffectionScore = 0;
let previousLevel = 1;
let chatBgBase = null;
let characterFaceUrl = null;
let rootEl;

function $(sel, parent = document) {
  return (parent || rootEl).querySelector(sel);
}

function setLoading(loading) {
  const icon = $(SELECTORS.sendIcon);
  const loadingEl = $(SELECTORS.sendLoading);
  if (icon) icon.hidden = loading;
  if (loadingEl) loadingEl.hidden = !loading;
  const sendBtn = $(SELECTORS.sendBtn);
  if (sendBtn) sendBtn.disabled = loading;
  if (rootEl) {
    if (loading) rootEl.classList.add('chat-sending');
    else rootEl.classList.remove('chat-sending');
  }
}

function setInputEnabled(enabled) {
  const input = $(SELECTORS.input);
  if (input) input.disabled = !enabled;
  const sendBtn = $(SELECTORS.sendBtn);
  if (sendBtn) sendBtn.disabled = !enabled;
}

function updateStatusBar(affectionScore, level) {
  const affectionValueEl = $(SELECTORS.statusAffectionValue);
  const levelEl = $(SELECTORS.statusLevel);
  if (affectionValueEl) affectionValueEl.textContent = affectionScore ?? 0;
  if (levelEl) levelEl.textContent = `Lv.${level ?? 1}`;
}

function setChatBackground(mood) {
  if (!chatBgBase) return;
  const url = getChatBgUrl(chatBgBase, mood);
  if (!url) return;
  const bg = $(SELECTORS.bg);
  if (bg) bg.style.backgroundImage = `url(${url})`;
}

function showAffectionDelta(delta, newLevel) {
  const el = $(SELECTORS.affection);
  const deltaEl = $(SELECTORS.affectionDelta);
  const levelUpEl = $(SELECTORS.levelUp);
  const statusAffection = $(SELECTORS.statusAffection);
  const statusLevel = $(SELECTORS.statusLevel);
  if (!el) return;
  el.hidden = false;
  if (deltaEl) {
    deltaEl.className = '';
    deltaEl.classList.remove(
      'chat-dialog__affection--up',
      'chat-dialog__affection--same',
      'chat-dialog__affection--down',
    );
    if (delta > 0) {
      deltaEl.classList.add('chat-dialog__affection--up');
      deltaEl.textContent = `↑ 호감도 +${delta}`;
    } else if (delta < 0) {
      deltaEl.classList.add('chat-dialog__affection--down');
      deltaEl.textContent = `↓ 호감도 ${delta}`;
    } else {
      deltaEl.classList.add('chat-dialog__affection--same');
      deltaEl.textContent = '→ 호감도 유지';
    }
  }
  if (levelUpEl) {
    if (newLevel != null && newLevel > 0) {
      levelUpEl.textContent = `↑ Lv.${newLevel} 달성!`;
      levelUpEl.hidden = false;
    } else {
      levelUpEl.hidden = true;
    }
  }
  if (statusAffection && (delta > 0 || delta < 0)) {
    statusAffection.classList.remove('affection-pulse');
    void statusAffection.offsetWidth;
    statusAffection.classList.add('affection-pulse');
    setTimeout(() => statusAffection.classList.remove('affection-pulse'), 550);
  }
  if (statusLevel && newLevel != null && newLevel > 0) {
    statusLevel.classList.remove('level-up-flash');
    void statusLevel.offsetWidth;
    statusLevel.classList.add('level-up-flash');
    setTimeout(() => statusLevel.classList.remove('level-up-flash'), 650);
  }
}

/** 현재 응답만 표시 (히스토리 없음), 3.2 AI 메시지 페이드인 연출 */
function setDialogMessage(aiMessage) {
  const container = $(SELECTORS.messages);
  if (!container) return;
  const text = aiMessage || '';
  if (!text) {
    container.textContent = '';
    if (rootEl) rootEl.classList.remove('chat-has-message');
    return;
  }
  if (rootEl) rootEl.classList.add('chat-has-message');
  const inner = document.createElement('span');
  inner.className = 'chat-dialog__message-inner';
  inner.textContent = text;
  container.innerHTML = '';
  container.appendChild(inner);
  container.classList.remove('chat-dialog__messages--animate');
  void container.offsetWidth;
  container.classList.add('chat-dialog__messages--animate');
  setTimeout(
    () => container.classList.remove('chat-dialog__messages--animate'),
    400,
  );
}

function showChoiceModal(aiMessage, choices) {
  const modal = $(SELECTORS.choiceModal);
  const messageEl = $(SELECTORS.choiceMessage);
  const buttonsEl = $(SELECTORS.choiceButtons);
  if (!modal || !messageEl || !buttonsEl) return;
  messageEl.textContent = aiMessage || '';
  buttonsEl.innerHTML = (choices || [])
    .map(
      (text) =>
        `<button type="button" class="chat-choice-modal__btn" data-choice>${escapeHtml(text)}</button>`,
    )
    .join('');
  modal.hidden = false;
  setInputEnabled(false);
  buttonsEl.querySelectorAll('[data-choice]').forEach((btn) => {
    btn.addEventListener('click', () => sendMessage(btn.textContent.trim()));
  });
}

function hideChoiceModal() {
  const modal = $(SELECTORS.choiceModal);
  if (modal) modal.hidden = true;
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}
function escapeAttr(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML.replace(/"/g, '&quot;');
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
    if (delta > 0) setChatBackground('happy');
    else if (delta < 0) setChatBackground('sad');
    else setChatBackground('neutral');
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

const HOME_URL = '/';
const HISTORY_PAGE_SIZE = 30;

let historyNextPage = 0;
let historyHasMore = true;
let historyLoading = false;

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

function formatBubbleTime(isoString) {
  if (!isoString) return '';
  const d = new Date(isoString);
  const now = new Date();
  const isToday = d.toDateString() === now.toDateString();
  if (isToday) {
    return d.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });
  }
  return (
    d.toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' }) +
    ' ' +
    d.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    })
  );
}

function renderHistoryBubble(log) {
  const isUser = log.speaker === 'USER';
  const time = formatBubbleTime(log.createdAt);
  const msg = escapeHtml(log.message || '');
  if (isUser) {
    return `<div class="chat-history-bubble chat-history-bubble--user">${msg}<span class="chat-history-bubble__time">${escapeHtml(time)}</span></div>`;
  }
  const avatarSrc = characterFaceUrl ? escapeAttr(characterFaceUrl) : '';
  const avatarHtml = characterFaceUrl
    ? `<img class="chat-history-bubble__avatar" src="${avatarSrc}" alt="" />`
    : '';
  return `<div class="chat-history-bubble chat-history-bubble--ai">${avatarHtml}<div class="chat-history-bubble__content">${msg}<span class="chat-history-bubble__time">${escapeHtml(time)}</span></div></div>`;
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
    const { content, hasNext } = await getChatLogs(
      soulmateId,
      historyNextPage,
      HISTORY_PAGE_SIZE,
    );
    historyHasMore = hasNext;
    historyNextPage += 1;
    const listEl = $(SELECTORS.historyList);
    if (!listEl) return;
    const reversed = [...content].reverse();
    const html = reversed.map(renderHistoryBubble).join('');
    const scrollEl = $(SELECTORS.historyScroll);
    if (prepend) {
      const prevScrollHeight = scrollEl ? scrollEl.scrollHeight : 0;
      const prevScrollTop = scrollEl ? scrollEl.scrollTop : 0;
      listEl.insertAdjacentHTML('afterbegin', html);
      requestAnimationFrame(() => {
        if (scrollEl)
          scrollEl.scrollTop =
            scrollEl.scrollHeight - prevScrollHeight + prevScrollTop;
      });
    } else {
      listEl.innerHTML = html;
      requestAnimationFrame(() => {
        if (scrollEl) scrollEl.scrollTop = scrollEl.scrollHeight;
      });
    }
  } catch (e) {
    if (listEl)
      listEl.insertAdjacentHTML(
        'afterbegin',
        `<p class="chat-history-modal__load">${escapeHtml(e.message || '불러오기 실패')}</p>`,
      );
  } finally {
    historyLoading = false;
    showHistoryLoad(false);
  }
}

function onHistoryScroll() {
  const scrollEl = $(SELECTORS.historyScroll);
  if (!scrollEl || !historyHasMore || historyLoading) return;
  if (scrollEl.scrollTop < 120) loadHistoryPage(true);
}

function openHistoryModal() {
  const modal = $(SELECTORS.historyModal);
  if (!modal) return;
  historyNextPage = 0;
  historyHasMore = true;
  modal.hidden = false;
  loadHistoryPage(false);
  const scrollEl = $(SELECTORS.historyScroll);
  if (scrollEl) {
    scrollEl.scrollTop = scrollEl.scrollHeight;
    scrollEl.addEventListener('scroll', onHistoryScroll, { passive: true });
  }
}

function closeHistoryModal() {
  const modal = $(SELECTORS.historyModal);
  const scrollEl = $(SELECTORS.historyScroll);
  if (scrollEl) scrollEl.removeEventListener('scroll', onHistoryScroll);
  if (modal) modal.hidden = true;
}

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
      previousAffectionScore = profile.affectionScore ?? 0;
      previousLevel = profile.level ?? 1;
      updateStatusBar(previousAffectionScore, previousLevel);
      chatBgBase = getChatBgBase(profile.characterImageId);
      characterFaceUrl = getCharacterFaceUrl(profile.characterImageId);
      const bg = $(SELECTORS.bg);
      if (bg && chatBgBase) {
        const url = getChatBgUrl(chatBgBase, 'neutral');
        if (url) bg.style.backgroundImage = `url(${url})`;
      }
      requestAnimationFrame(() => {
        rootEl.classList.add('chat-entered');
      });
    })
    .catch(() => {
      // 404 등: 캐릭터가 없으면 홈으로 보내서 캐릭터 생성 유도
      window.location.replace(HOME_URL);
      return;
    });

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

  const hamburger = $(SELECTORS.hamburger);
  const menuHome = $(SELECTORS.menuHome);
  const menuHistory = $(SELECTORS.menuHistory);
  if (hamburger) hamburger.addEventListener('click', () => toggleMenu());
  if (menuHome) menuHome.addEventListener('click', () => closeMenu());
  if (menuHistory) {
    menuHistory.addEventListener('click', () => {
      closeMenu();
      openHistoryModal();
    });
  }

  const historyBackdrop = $(SELECTORS.historyBackdrop);
  const historyClose = $(SELECTORS.historyClose);
  if (historyBackdrop)
    historyBackdrop.addEventListener('click', closeHistoryModal);
  if (historyClose) historyClose.addEventListener('click', closeHistoryModal);
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
