/**
 * 캐릭터 생성 페이지 — 4-Step 마법사 (Step 인디케이터 + 챔버 라이팅)
 *
 *  Step 1: 이름 + 성별 (FEMALE / MALE — 보정③ OTHER 클라이언트 미노출)
 *  Step 2: 캐릭터 선택 (.smcard 풀 마크업, 테마 inline 변수, voice preview placeholder)
 *  Step 3: 성격 · 취미 · 말투 (.chip-section, 각 최대 3개)
 *  Step 4: SUMMARY 카드 (보정② 나이 카피 제거 + 보정⑤ 라벨 ' · ' 으로 연결)
 */
import { createSoulmate } from '../../api.js';
import { CONFIG } from '../../config.js';
import {
  CHARACTER_IMAGES_BY_GENDER,
  CHARACTER_DEFS,
  PERSONALITY_OPTIONS,
  HOBBIES_OPTIONS,
  SPEECH_OPTIONS,
  STEP_TITLES,
  STEP_EYEBROWS,
  getCharacterDef,
} from './options.js';

const SELECTORS = {
  root: '[data-app-root]',
  create: '.create',
  back: '[data-create-back]',
  title: '[data-create-title]',
  eyebrow: '[data-create-eyebrow]',
  stepIndicator: '[data-create-step-indicator]',
  stepDot: '[data-step-dot]',
  stepSections: '.create-step[data-step]',
  nextBtn: '[data-create-next]',
  submitBtn: '[data-create-submit]',
  // Step 1
  name: '[data-create-name]',
  gender: '[data-create-gender]',
  genderBtn: '[data-gender]',
  // Step 2
  imageGrid: '[data-create-image-grid]',
  // Step 3
  personality: '[data-create-personality]',
  personalityChips: '[data-create-personality-chips]',
  personalityCounter: '[data-create-personality-counter]',
  hobbies: '[data-create-hobbies]',
  hobbiesChips: '[data-create-hobbies-chips]',
  hobbiesCounter: '[data-create-hobbies-counter]',
  speech: '[data-create-speech]',
  speechChips: '[data-create-speech-chips]',
  speechCounter: '[data-create-speech-counter]',
  // Step 4
  summary: '[data-create-summary]',
  customPersonality: '[data-custom-personality]',
  customPersonalityChips: '[data-custom-personality-chips]',
  customHobby: '[data-custom-hobby]',
  customHobbyChips: '[data-custom-hobby-chips]',
  customSpeech: '[data-custom-speech]',
  customSpeechChips: '[data-custom-speech-chips]',
  addPersonality: '[data-add-personality]',
  addHobby: '[data-add-hobby]',
  addSpeech: '[data-add-speech]',
};

const MAX_CHIP_SELECT = 3;
const THEME_VARS = ['primary', 'secondary', 'accent', 'deep', 'mist', 'glow'];

const state = {
  step: 1,
  name: '',
  gender: '',
  characterImageId: '',
  characterImageUrl: '',
  /** Day 7 Step 8 — 5트랙 외모 선택 ⑤ 커스텀 트랙 전용. 프리셋 트랙일 땐 빈 문자열. */
  customAppearancePrompt: '',
  personalityKeywords: [],
  hobbies: [],
  speechStyles: [],
  /** Step 4 커스텀 추가 — 카테고리당 1개 */
  customPersonality: '',
  customHobby: '',
  customSpeech: '',
};

let rootEl;
let createEl;
let stepSections;
let indicatorDots;

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
// 테마 변수 적용 (.create 컨테이너 inline, root.smcard 도 자식이므로 자동 전파)
// ============================================================
function applyTheme(themeKey) {
  if (!createEl) return;
  const key = themeKey || 'bright';
  THEME_VARS.forEach((v) => {
    createEl.style.setProperty(`--theme-${v}`, `var(--theme-${key}-${v})`);
  });
}

// ============================================================
// Step 전환 — section [hidden] + indicator dot 클래스 + eyebrow/title
// ============================================================
function showStep(step) {
  state.step = step;
  if (createEl) createEl.setAttribute('data-step', String(step));

  stepSections.forEach((el) => {
    const n = parseInt(el.getAttribute('data-step'), 10);
    if (!Number.isNaN(n)) el.hidden = n !== step;
  });
  indicatorDots.forEach((el) => {
    const n = parseInt(el.getAttribute('data-step-dot'), 10);
    el.classList.toggle('create__step-dot--active', n === step);
    el.classList.toggle('create__step-dot--done', n < step);
  });

  const titleEl = $(SELECTORS.title, rootEl);
  const eyebrowEl = $(SELECTORS.eyebrow, rootEl);
  if (titleEl) titleEl.textContent = STEP_TITLES[step] || 'Create New Soulmate';
  if (eyebrowEl) eyebrowEl.textContent = STEP_EYEBROWS[step] || '';

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
  if (step === 1) return !!state.gender && !!state.name.trim();
  if (step === 2) {
    if (!state.characterImageId) return false;
    // Day 7 Step 8 — 커스텀 트랙은 외모 prompt 4자 이상 필수
    if (state.characterImageId === 'custom') {
      return state.customAppearancePrompt.trim().length >= 4;
    }
    return true;
  }
  if (step === 3) {
    return (
      state.personalityKeywords.length >= 1 &&
      state.hobbies.length >= 1 &&
      state.speechStyles.length >= 1
    );
  }
  if (step === 4) return true;
  return false;
}

// ============================================================
// Step 2 — 5트랙 외모 선택 (Day 7 retrofit, Claude Design handoff 통합)
//   ① ~ ④ 프리셋 (정적 마크업, .app-card)
//   ⑤ 커스텀 (.app-card--custom + .app-prompt 펼침 textarea)
// ============================================================
function renderStep2() {
  const grid = $(SELECTORS.imageGrid, rootEl);
  if (!grid) return;

  // 정적 마크업이라 *innerHTML 재작성* 안 함. 카드 선택 상태만 동기화.
  const cards = grid.querySelectorAll('[data-app-card]');
  cards.forEach((card) => {
    const id = card.getAttribute('data-image-id');
    const isSelected = state.characterImageId === id;
    card.classList.toggle('app-card--selected', isSelected);
    card.setAttribute('aria-checked', isSelected ? 'true' : 'false');
  });

  // 커스텀 prompt 펼침 영역 — 커스텀 카드 선택 시 펼침
  const promptBox = grid.querySelector('[data-app-prompt]');
  if (promptBox) {
    promptBox.dataset.open = state.characterImageId === 'custom' ? 'true' : 'false';
  }

  // 텍스트영역 / 카운터 / 다음 버튼 동기화
  const promptTextarea = grid.querySelector('[data-custom-prompt]');
  const promptCount = grid.querySelector('[data-prompt-count]');
  if (promptTextarea && promptTextarea.value !== state.customAppearancePrompt) {
    promptTextarea.value = state.customAppearancePrompt;
  }
  if (promptCount) {
    const len = state.customAppearancePrompt.length;
    promptCount.textContent = `${len} / 200`;
    promptCount.dataset.nearLimit = len > 160 ? 'true' : 'false';
  }
}

/**
 * Step 2 의 5트랙 그리드 클릭 + 커스텀 prompt 입력 핸들러를 *한 번만* 바인드.
 * 정적 마크업이라 init() 시점에 한 번만 박으면 됨.
 */
function bindStep2Handlers() {
  const grid = $(SELECTORS.imageGrid, rootEl);
  if (!grid) return;

  // 카드 클릭 → state 갱신 + 테마 적용 + 동기화
  grid.addEventListener('click', (e) => {
    const card = e.target.closest('[data-app-card]');
    if (!card) return;
    const id = card.getAttribute('data-image-id');
    state.characterImageId = id;
    if (id === 'custom') {
      state.characterImageUrl = '';
      // 커스텀일 땐 *bright* 테마를 임시로 (스파클/노란 결) 적용
      applyTheme('bright');
      // textarea 포커스
      setTimeout(() => {
        const ta = grid.querySelector('[data-custom-prompt]');
        if (ta) ta.focus({ preventScroll: true });
      }, 280);
    } else {
      state.characterImageUrl = card.getAttribute('data-image-url') || '';
      // 카드 자신의 data-theme 으로 테마 갱신 (cheerful/calm/warm/bright)
      const theme = card.getAttribute('data-theme');
      if (theme) applyTheme(theme);
    }
    renderStep2();
    syncNextBtn();
  });

  // 커스텀 prompt 입력 → state 갱신
  const promptTextarea = grid.querySelector('[data-custom-prompt]');
  if (promptTextarea) {
    promptTextarea.addEventListener('input', () => {
      state.customAppearancePrompt = promptTextarea.value;
      const promptCount = grid.querySelector('[data-prompt-count]');
      if (promptCount) {
        const len = state.customAppearancePrompt.length;
        promptCount.textContent = `${len} / 200`;
        promptCount.dataset.nearLimit = len > 160 ? 'true' : 'false';
      }
      syncNextBtn();
    });
  }
}

/** 현재 step 의 다음 버튼 disabled 동기화 — bindStep2Handlers 의 textarea 입력 등에서 호출. */
function syncNextBtn() {
  const nextBtn = $(SELECTORS.nextBtn, rootEl);
  if (nextBtn) nextBtn.disabled = !canProceed(state.step);
  const submitBtn = $(SELECTORS.submitBtn, rootEl);
  if (submitBtn && state.step === 4) submitBtn.disabled = !canProceed(4);
}

// ============================================================
// Step 3 — 칩 섹션 (.chip-grid 안에 .chip 들 + counter 갱신)
// ============================================================
function renderChips(sectionEl, options, stateKey, counterEl) {
  if (!sectionEl) return;
  const gridEl = sectionEl.querySelector('.chip-grid');
  if (!gridEl) return;
  const selected = state[stateKey] || [];

  gridEl.innerHTML = options
    .map(
      (opt) => `
        <button type="button" class="chip ${selected.includes(opt.label) ? 'chip--selected' : ''}"
                data-chip-id="${escapeAttr(opt.id)}"
                data-label="${escapeAttr(opt.label)}"
                title="${escapeAttr(opt.label)}">
          <span class="chip__emoji" aria-hidden="true">${escapeHtml(opt.emoji)}</span>
          <span>${escapeHtml(opt.label)}</span>
        </button>`,
    )
    .join('');

  updateCounter(stateKey, counterEl);

  gridEl.querySelectorAll('.chip').forEach((btn) => {
    btn.addEventListener('click', () => {
      const label = btn.getAttribute('data-label');
      const set = new Set(state[stateKey]);
      if (set.has(label)) {
        set.delete(label);
      } else {
        if (set.size >= MAX_CHIP_SELECT) return;
        set.add(label);
      }
      state[stateKey] = Array.from(set);
      renderChips(sectionEl, options, stateKey, counterEl);
      if (state.step === 3) {
        const nextBtn = $(SELECTORS.nextBtn, rootEl);
        if (nextBtn) nextBtn.disabled = !canProceed(3);
      }
    });
  });
}

function updateCounter(stateKey, counterEl) {
  if (!counterEl) return;
  const count = state[stateKey].length;
  counterEl.textContent = `${count} / ${MAX_CHIP_SELECT}`;
  counterEl.classList.toggle('chip-section__counter--full', count >= MAX_CHIP_SELECT);
}

// ============================================================
// Step 4 — Summary 카드 (보정② 나이 제거 / 보정⑤ 라벨 ' · ' 으로 연결)
// ============================================================
function renderStep4Summary() {
  const summaryEl = $(SELECTORS.summary, rootEl);
  if (!summaryEl) return;
  const def = getCharacterDef(state.characterImageId);
  const name = state.name.trim() || def.name;
  const portraitImage = state.characterImageId
    ? `url('/images/characters/${state.characterImageId}-face.jpg')`
    : `url('/images/characters/character-female-bright-face.jpg')`;

  // 라벨들 ' · ' 으로 연결 (보정⑤ 어미 처리 대신 미연시 톤의 단순 나열)
  const personality = [...state.personalityKeywords];
  if (state.customPersonality) personality.push(state.customPersonality);
  const hobbies = [...state.hobbies];
  if (state.customHobby) hobbies.push(state.customHobby);
  const speech = [...state.speechStyles];
  if (state.customSpeech) speech.push(state.customSpeech);

  const personalityText = personality.length ? personality.join(' · ') : '특별한';
  const hobbyText       = hobbies.length     ? hobbies.join(' · ')     : '여러 가지';
  const speechText      = speech.length      ? speech.join(' · ')      : '편한 말투';

  // 테마 + portrait 변수
  THEME_VARS.forEach((v) => {
    summaryEl.style.setProperty(`--theme-${v}`, `var(--theme-${def.themeKey}-${v})`);
  });
  summaryEl.style.setProperty('--portrait-image', portraitImage);

  // 보정②: '${age}세' 같은 나이 카피는 빼고, 이름·무드만.
  summaryEl.innerHTML = `
    <div class="summary-card__cut" aria-hidden="true"></div>
    <div class="summary-card__face" aria-hidden="true"></div>
    <div class="summary-card__nametag">${escapeHtml(name)} · ${escapeHtml(def.mood)}</div>
    <div class="summary-card__body">
      안녕, 나는 <strong>${escapeHtml(name)}</strong>이야.
      <em>${escapeHtml(personalityText)}</em> 분위기를 좋아하고,
      ${escapeHtml(hobbyText)} 즐겨하고,
      평소엔 <em>${escapeHtml(speechText)}</em> 로 편하게 이야기해.
    </div>
  `;
}

// ============================================================
// Custom row — 카테고리당 1개, 칩 미리보기 .custom-row__chips 에 표시
// ============================================================
function syncCustomRow(value, inputSel, chipsSel) {
  const input = $(inputSel, rootEl);
  const chipsEl = $(chipsSel, rootEl);
  if (input) input.value = value;
  if (chipsEl) {
    if (value) {
      chipsEl.innerHTML = `
        <span class="chip chip--selected chip--custom" data-custom-chip>
          <span>${escapeHtml(value)}</span>
          <span style="opacity:0.7" aria-hidden="true">×</span>
        </span>`;
      chipsEl.parentElement?.classList.add('custom-row--open');
    } else {
      chipsEl.innerHTML = '';
      chipsEl.parentElement?.classList.remove('custom-row--open');
    }
  }
}

function syncAllCustomRows() {
  syncCustomRow(state.customPersonality, SELECTORS.customPersonality, SELECTORS.customPersonalityChips);
  syncCustomRow(state.customHobby,        SELECTORS.customHobby,        SELECTORS.customHobbyChips);
  syncCustomRow(state.customSpeech,       SELECTORS.customSpeech,       SELECTORS.customSpeechChips);
}

function bindCustomAdd(stateKey, inputSel, addBtnSel, chipsSel) {
  const input = $(inputSel, rootEl);
  const addBtn = $(addBtnSel, rootEl);
  const chipsEl = $(chipsSel, rootEl);
  if (!input || !addBtn) return;
  addBtn.addEventListener('click', () => {
    const v = input.value.trim();
    if (!v) {
      // 비었으면 기존 값 제거 (toggle)
      state[stateKey] = '';
    } else {
      state[stateKey] = v;
    }
    syncCustomRow(state[stateKey], inputSel, chipsSel);
    renderStep4Summary();
  });
  // 미리보기 칩의 × 클릭으로 제거
  if (chipsEl) {
    chipsEl.addEventListener('click', (e) => {
      const chip = e.target.closest('[data-custom-chip]');
      if (!chip) return;
      state[stateKey] = '';
      input.value = '';
      syncCustomRow('', inputSel, chipsSel);
      renderStep4Summary();
    });
  }
}

// ============================================================
// 제출 payload
// ============================================================
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
    name: state.name.trim() || undefined,
    personalityKeywords: personality,
    hobbies,
    speechStyles: speech,
    // Day 7 Step 8 — 5트랙 외모 선택의 ⑤ 커스텀 트랙 전용. 프리셋 트랙일 땐 null 로 흘려 백엔드가 분기.
    customAppearancePrompt: state.characterImageId === 'custom'
      ? (state.customAppearancePrompt.trim() || null)
      : null,
  };
}

// ============================================================
// 초기화
// ============================================================
function init() {
  rootEl = document.querySelector(SELECTORS.root);
  if (!rootEl) return;
  createEl = $(SELECTORS.create, rootEl);
  stepSections = $$(SELECTORS.stepSections, rootEl);
  indicatorDots = $$(SELECTORS.stepDot, rootEl);

  // Step 1: 이름·성별
  const nameInput = $(SELECTORS.name, rootEl);
  if (nameInput) {
    nameInput.value = state.name;
    nameInput.addEventListener('input', () => {
      state.name = nameInput.value;
      const nextBtn = $(SELECTORS.nextBtn, rootEl);
      if (nextBtn && state.step === 1) nextBtn.disabled = !canProceed(1);
    });
  }

  const genderContainer = $(SELECTORS.gender, rootEl);
  if (genderContainer) {
    genderContainer.querySelectorAll(SELECTORS.genderBtn).forEach((btn) => {
      btn.addEventListener('click', () => {
        state.gender = btn.getAttribute('data-gender') || '';
        genderContainer
          .querySelectorAll(SELECTORS.genderBtn)
          .forEach((b) => b.classList.toggle('create__gender--selected', b === btn));
        // 성별이 바뀌면 캐릭터 선택 초기화
        if (!CHARACTER_IMAGES_BY_GENDER[state.gender]?.some((c) => c.id === state.characterImageId)) {
          state.characterImageId = '';
          state.characterImageUrl = '';
        }
        const nextBtn = $(SELECTORS.nextBtn, rootEl);
        if (nextBtn) nextBtn.disabled = !canProceed(1);
      });
    });
  }

  // 뒤로가기 — Step 진행 중이면 이전 Step 으로, Step 1 이면 홈으로
  const backEl = $(SELECTORS.back, rootEl);
  if (backEl) {
    backEl.addEventListener('click', (e) => {
      if (state.step > 1) {
        e.preventDefault();
        const prev = state.step - 1;
        showStep(prev);
        if (prev === 2) renderStep2();
        if (prev === 4) renderStep4Summary();
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
        renderChips($(SELECTORS.personality, rootEl), PERSONALITY_OPTIONS, 'personalityKeywords', $(SELECTORS.personalityCounter, rootEl));
        renderChips($(SELECTORS.hobbies,     rootEl), HOBBIES_OPTIONS,     'hobbies',             $(SELECTORS.hobbiesCounter,     rootEl));
        renderChips($(SELECTORS.speech,      rootEl), SPEECH_OPTIONS,      'speechStyles',        $(SELECTORS.speechCounter,      rootEl));
      } else if (state.step === 3) {
        showStep(4);
        syncAllCustomRows();
        renderStep4Summary();
      }
    });
  }

  // 만들기
  const submitBtn = $(SELECTORS.submitBtn, rootEl);
  if (submitBtn) {
    submitBtn.addEventListener('click', async () => {
      const payload = buildPayload();
      submitBtn.disabled = true;
      // Day 7 Step 8 — 커스텀 트랙은 백엔드에서 ImageGenerationService 가 1회 호출되므로 약 30초 대기.
      // 사용자 경험: 만들기 버튼 텍스트를 *얼굴 빚는 중* 으로 갈아 미연시 톤 유지.
      const isCustom = payload.characterImageId === 'custom';
      const submitText = submitBtn.querySelector('span:nth-of-type(2)');
      const originalText = submitText ? submitText.textContent : '';
      if (isCustom && submitText) {
        submitText.textContent = '얼굴 빚는 중…';
      }
      try {
        const res = await createSoulmate(payload);
        window.location.href = CONFIG.routes.chat(res.id);
      } catch (err) {
        alert(err.message || '생성에 실패했어요');
        if (isCustom && submitText) submitText.textContent = originalText;
        submitBtn.disabled = false;
      }
    });
  }

  // Day 7 Step 8 — 5트랙 외모 그리드 클릭 / 커스텀 prompt 입력 핸들러를 *한 번만* 바인드
  bindStep2Handlers();

  // Step 4 커스텀 추가/제거 핸들러
  bindCustomAdd('customPersonality', SELECTORS.customPersonality, SELECTORS.addPersonality, SELECTORS.customPersonalityChips);
  bindCustomAdd('customHobby',        SELECTORS.customHobby,        SELECTORS.addHobby,        SELECTORS.customHobbyChips);
  bindCustomAdd('customSpeech',       SELECTORS.customSpeech,       SELECTORS.addSpeech,       SELECTORS.customSpeechChips);

  showStep(1);
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
