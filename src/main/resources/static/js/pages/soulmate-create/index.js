/**
 * 캐릭터 생성 페이지 — Step 플로우, api 연동
 */
import { createSoulmate } from '../../api.js';
import { CONFIG } from '../../config.js';
import {
  CHARACTER_IMAGES_BY_GENDER,
  PERSONALITY_OPTIONS,
  HOBBIES_OPTIONS,
  SPEECH_OPTIONS,
  STEP_TITLES,
} from './options.js';

const SELECTORS = {
  back: '[data-create-back]',
  title: '[data-create-title]',
  stepIndicator: '[data-create-step-indicator]',
  stepDot: '[data-step-dot]',
  nextBtn: '[data-create-next]',
  submitBtn: '[data-create-submit]',
  name: '[data-create-name]',
  gender: '[data-create-gender]',
  genderBtn: '[data-gender]',
  imageGrid: '[data-create-image-grid]',
  personality: '[data-create-personality]',
  hobbies: '[data-create-hobbies]',
  speech: '[data-create-speech]',
  summary: '[data-create-summary]',
  customPersonality: '[data-custom-personality]',
  customHobby: '[data-custom-hobby]',
  customSpeech: '[data-custom-speech]',
  addPersonality: '[data-add-personality]',
  addHobby: '[data-add-hobby]',
  addSpeech: '[data-add-speech]',
};

const state = {
  step: 1,
  name: '',
  gender: '',
  characterImageId: '',
  characterImageUrl: '',
  personalityKeywords: [],
  hobbies: [],
  speechStyles: [],
  /** Step 4 커스텀 추가 — 카테고리당 1개만 */
  customPersonality: '',
  customHobby: '',
  customSpeech: '',
};

let rootEl;
let stepSections;
let indicatorDots;

function $(sel, parent = document) {
  return parent.querySelector(sel);
}
function $$(sel, parent = document) {
  return Array.from(parent.querySelectorAll(sel));
}

function showStep(step) {
  state.step = step;
  stepSections.forEach((el) => {
    const n = parseInt(el.getAttribute('data-step'), 10);
    el.hidden = n !== step;
  });
  indicatorDots.forEach((el) => {
    const n = parseInt(el.getAttribute('data-step-dot'), 10);
    el.classList.toggle('is-active', n === step);
  });

  const titleEl = $(SELECTORS.title, rootEl);
  if (titleEl) titleEl.textContent = STEP_TITLES[step] || '새 이성친구 만들기';

  const nextBtn = $(SELECTORS.nextBtn, rootEl);
  const submitBtn = $(SELECTORS.submitBtn, rootEl);
  if (nextBtn) {
    nextBtn.hidden = step === 4;
    nextBtn.disabled = !canProceed(step);
  }
  if (submitBtn) {
    submitBtn.hidden = step !== 4;
    submitBtn.disabled = !canProceed(4);
  }
}

function canProceed(step) {
  if (step === 1) return !!state.gender;
  if (step === 2) return !!state.characterImageId;
  if (step === 3)
    return (
      state.personalityKeywords.length >= 1 &&
      state.hobbies.length >= 1 &&
      state.speechStyles.length >= 1
    );
  if (step === 4) return true;
  return false;
}

function renderStep2() {
  const grid = $(SELECTORS.imageGrid, rootEl);
  if (!grid || !state.gender) return;
  const list = CHARACTER_IMAGES_BY_GENDER[state.gender] || [];
  grid.innerHTML = list
    .map(
      (img) => `
    <button type="button" class="create-image-card ${state.characterImageId === img.id ? 'is-selected' : ''}" 
            data-image-id="${escapeAttr(img.id)}" data-image-url="${escapeAttr(img.url || '')}">
      <img class="create-image-card__img" src="${escapeAttr(img.url)}" alt="" loading="lazy" />
    </button>
  `,
    )
    .join('');

  grid.querySelectorAll('.create-image-card').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.characterImageId = btn.getAttribute('data-image-id');
      state.characterImageUrl = btn.getAttribute('data-image-url') || '';
      renderStep2();
      const nextBtn = $(SELECTORS.nextBtn, rootEl);
      if (nextBtn) nextBtn.disabled = false;
    });
  });
}

function renderChips(container, options, stateKey) {
  if (!container) return;
  const selected = state[stateKey] || [];
  container.innerHTML = options
    .map(
      (opt) => `
    <button type="button" class="create-chip ${selected.includes(opt.label) ? 'is-selected' : ''}" 
            data-label="${escapeAttr(opt.label)}" title="${escapeAttr(opt.label)}">
      <span class="create-chip__emoji">${escapeHtml(opt.emoji)}</span>
      <span class="create-chip__label">${escapeHtml(opt.label)}</span>
    </button>
  `,
    )
    .join('');

  const MAX_CHIP_SELECT = 3;

  container.querySelectorAll('.create-chip').forEach((btn) => {
    btn.addEventListener('click', () => {
      const label = btn.getAttribute('data-label');
      const set = new Set(state[stateKey]);
      if (set.has(label)) {
        set.delete(label);
      } else {
        if (set.size >= MAX_CHIP_SELECT) return; /* 최대 3개 */
        set.add(label);
      }
      state[stateKey] = Array.from(set);
      renderChips(container, options, stateKey);
      if (state.step === 3) {
        const nextBtn = $(SELECTORS.nextBtn, rootEl);
        if (nextBtn) nextBtn.disabled = !canProceed(3);
      }
    });
  });
}

function renderStep4Summary() {
  const summaryEl = $(SELECTORS.summary, rootEl);
  if (!summaryEl) return;
  const genderLabel =
    { FEMALE: '여성', MALE: '남성' }[state.gender] ||
    state.gender;
  const personality = [...state.personalityKeywords];
  if (state.customPersonality) personality.push(state.customPersonality);
  const hobbies = [...state.hobbies];
  if (state.customHobby) hobbies.push(state.customHobby);
  const speech = [...state.speechStyles];
  if (state.customSpeech) speech.push(state.customSpeech);

  summaryEl.innerHTML = `
    <div class="create-summary__row"><span class="create-summary__key">이름</span>${escapeHtml(state.name || '—')}</div>
    <div class="create-summary__row"><span class="create-summary__key">성별</span>${escapeHtml(genderLabel)}</div>
    <div class="create-summary__row"><span class="create-summary__key">성격</span>${escapeHtml(personality.join(', ') || '—')}</div>
    <div class="create-summary__row"><span class="create-summary__key">취미</span>${escapeHtml(hobbies.join(', ') || '—')}</div>
    <div class="create-summary__row"><span class="create-summary__key">말투</span>${escapeHtml(speech.join(', ') || '—')}</div>
  `;
}

function buildPayload() {
  const personality = [...state.personalityKeywords];
  if (state.customPersonality) personality.push(state.customPersonality);
  const hobbies = [...state.hobbies];
  if (state.customHobby) hobbies.push(state.customHobby);
  const speech = [...state.speechStyles];
  if (state.customSpeech) speech.push(state.customSpeech);

  return {
    gender: state.gender,
    characterImageId: state.characterImageId,
    characterImageUrl: state.characterImageUrl || undefined,
    name: (state.name || '').trim() || undefined,
    personalityKeywords: personality,
    hobbies,
    speechStyles: speech,
  };
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}
function escapeAttr(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML.replace(/"/g, '&quot;');
}

function init() {
  rootEl = $(SELECTORS.back)?.closest('[data-app-root]');
  if (!rootEl) return;

  stepSections = $$('[data-step]', rootEl);
  indicatorDots = $$(SELECTORS.stepDot, rootEl);

  // Step 1: 이름·성별
  const nameInput = $(SELECTORS.name, rootEl);
  if (nameInput) {
    nameInput.value = state.name;
    nameInput.addEventListener('input', () => {
      state.name = nameInput.value;
    });
  }

  const genderContainer = $(SELECTORS.gender, rootEl);
  if (genderContainer) {
    genderContainer.querySelectorAll(SELECTORS.genderBtn).forEach((btn) => {
      btn.addEventListener('click', () => {
        state.gender = btn.getAttribute('data-gender') || '';
        genderContainer
          .querySelectorAll(SELECTORS.genderBtn)
          .forEach((b) => b.classList.remove('is-selected'));
        btn.classList.add('is-selected');
        const nextBtn = $(SELECTORS.nextBtn, rootEl);
        if (nextBtn) nextBtn.disabled = false;
      });
    });
  }

  // 뒤로가기
  const backEl = $(SELECTORS.back, rootEl);
  if (backEl) {
    backEl.addEventListener('click', (e) => {
      if (state.step > 1) {
        e.preventDefault();
        showStep(state.step - 1);
        if (state.step === 2) renderStep2();
        if (state.step === 4) renderStep4Summary();
      }
    });
  }

  // 다음
  const nextBtn = $(SELECTORS.nextBtn, rootEl);
  if (nextBtn) {
    nextBtn.addEventListener('click', () => {
      if (state.step === 1) {
        showStep(2);
        renderStep2();
      } else if (state.step === 2) {
        showStep(3);
        renderChips(
          $(SELECTORS.personality, rootEl),
          PERSONALITY_OPTIONS,
          'personalityKeywords',
        );
        renderChips($(SELECTORS.hobbies, rootEl), HOBBIES_OPTIONS, 'hobbies');
        renderChips(
          $(SELECTORS.speech, rootEl),
          SPEECH_OPTIONS,
          'speechStyles',
        );
      } else if (state.step === 3) {
        showStep(4);
        syncCustomRows();
        renderStep4Summary();
        const submitBtn = $(SELECTORS.submitBtn, rootEl);
        if (submitBtn) submitBtn.disabled = false;
      }
    });
  }

  // 만들기
  const submitBtn = $(SELECTORS.submitBtn, rootEl);
  if (submitBtn) {
    submitBtn.addEventListener('click', async () => {
      const payload = buildPayload();
      submitBtn.disabled = true;
      try {
        const res = await createSoulmate(payload);
        const url = CONFIG.routes.chat(res.id);
        window.location.href = url;
      } catch (err) {
        alert(err.message || '생성에 실패했어요');
        submitBtn.disabled = false;
      }
    });
  }

  // Step 4 커스텀 추가/수정 — 카테고리당 1개, 추가 후 버튼은 "수정"으로 변경해 재편집 가능
  function syncCustomRows() {
    const personalityInput = $(SELECTORS.customPersonality, rootEl);
    const personalityBtn = $(SELECTORS.addPersonality, rootEl);
    if (personalityInput && personalityBtn) {
      personalityInput.value = state.customPersonality;
      personalityBtn.textContent = state.customPersonality ? '수정' : '추가';
    }
    const hobbyInput = $(SELECTORS.customHobby, rootEl);
    const hobbyBtn = $(SELECTORS.addHobby, rootEl);
    if (hobbyInput && hobbyBtn) {
      hobbyInput.value = state.customHobby;
      hobbyBtn.textContent = state.customHobby ? '수정' : '추가';
    }
    const speechInput = $(SELECTORS.customSpeech, rootEl);
    const speechBtn = $(SELECTORS.addSpeech, rootEl);
    if (speechInput && speechBtn) {
      speechInput.value = state.customSpeech;
      speechBtn.textContent = state.customSpeech ? '수정' : '추가';
    }
  }

  function addOrUpdateCustom(key, inputSel, addBtnSel) {
    const input = $(inputSel, rootEl);
    const addBtn = $(addBtnSel, rootEl);
    if (!input || !addBtn) return;
    addBtn.addEventListener('click', () => {
      const v = input.value.trim();
      if (!v) return;
      if (key === 'customPersonality') state.customPersonality = v;
      else if (key === 'customHobby') state.customHobby = v;
      else if (key === 'customSpeech') state.customSpeech = v;
      syncCustomRows();
      renderStep4Summary();
    });
  }
  addOrUpdateCustom(
    'customPersonality',
    SELECTORS.customPersonality,
    SELECTORS.addPersonality,
  );
  addOrUpdateCustom('customHobby', SELECTORS.customHobby, SELECTORS.addHobby);
  addOrUpdateCustom(
    'customSpeech',
    SELECTORS.customSpeech,
    SELECTORS.addSpeech,
  );

  showStep(1);
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
