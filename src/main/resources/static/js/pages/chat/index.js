/**
 * 채팅 화면 — 미연시 스타일, 전송/로딩/다이얼로그/호감도/선택지 모달
 */
import { getSoulmate, postChat } from '../../api.js';

const SELECTORS = {
  root: '[data-app-root]',
  bg: '[data-chat-bg]',
  statusAffection: '[data-chat-status-affection]',
  statusLevel: '[data-chat-status-level]',
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
};

let soulmateId;
let previousAffectionScore = 0;
let previousLevel = 1;
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
}

function setInputEnabled(enabled) {
  const input = $(SELECTORS.input);
  if (input) input.disabled = !enabled;
  const sendBtn = $(SELECTORS.sendBtn);
  if (sendBtn) sendBtn.disabled = !enabled;
}

function updateStatusBar(affectionScore, level) {
  const affectionEl = $(SELECTORS.statusAffection);
  const levelEl = $(SELECTORS.statusLevel);
  if (affectionEl) affectionEl.textContent = `❤ ${affectionScore ?? 0}`;
  if (levelEl) levelEl.textContent = `Lv.${level ?? 1}`;
}

function showAffectionDelta(delta, newLevel) {
  const el = $(SELECTORS.affection);
  const deltaEl = $(SELECTORS.affectionDelta);
  const levelUpEl = $(SELECTORS.levelUp);
  if (!el) return;
  el.hidden = false;
  if (deltaEl) {
    deltaEl.className = '';
    deltaEl.classList.remove('chat-dialog__affection--up', 'chat-dialog__affection--same', 'chat-dialog__affection--down');
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
}

/** 현재 응답만 표시 (히스토리 없음) */
function setDialogMessage(aiMessage) {
  const container = $(SELECTORS.messages);
  if (!container) return;
  container.textContent = aiMessage || '';
}

function showChoiceModal(aiMessage, choices) {
  const modal = $(SELECTORS.choiceModal);
  const messageEl = $(SELECTORS.choiceMessage);
  const buttonsEl = $(SELECTORS.choiceButtons);
  if (!modal || !messageEl || !buttonsEl) return;
  messageEl.textContent = aiMessage || '';
  buttonsEl.innerHTML = (choices || []).map((text) => `<button type="button" class="chat-choice-modal__btn" data-choice>${escapeHtml(text)}</button>`).join('');
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

async function sendMessage(text) {
  if (!text || !soulmateId) return;
  setLoading(true);
  setInputEnabled(false);
  hideChoiceModal();
  try {
    const res = await postChat(soulmateId, text);
    const delta = res.affectionScore != null && previousAffectionScore != null ? res.affectionScore - previousAffectionScore : 0;
    const levelUp = res.level != null && previousLevel != null && res.level > previousLevel ? res.level : null;
    previousAffectionScore = res.affectionScore ?? previousAffectionScore;
    previousLevel = res.level ?? previousLevel;

    updateStatusBar(res.affectionScore, res.level);
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

function init() {
  rootEl = document.querySelector(SELECTORS.root);
  if (!rootEl) return;
  const id = rootEl.getAttribute('data-soulmate-id');
  soulmateId = id ? parseInt(id, 10) : null;
  if (!soulmateId) {
    alert('채팅 상대를 찾을 수 없어요.');
    return;
  }

  getSoulmate(soulmateId).then((profile) => {
    previousAffectionScore = profile.affectionScore ?? 0;
    previousLevel = profile.level ?? 1;
    updateStatusBar(previousAffectionScore, previousLevel);
    const bg = $(SELECTORS.bg);
    if (bg && profile.characterImageUrl) {
      bg.style.backgroundImage = `url(${profile.characterImageUrl})`;
    }
  }).catch(() => {});

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
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
