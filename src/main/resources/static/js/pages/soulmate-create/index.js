/**
 * 캐릭터 생성 페이지 — 4-Step 마법사 (Step 인디케이터 + 챔버 라이팅)
 *
 *  Step 1: 이름 + 성별 (FEMALE / MALE — 보정③ OTHER 클라이언트 미노출)
 *  Step 2: 캐릭터 선택 (.smcard 풀 마크업, 테마 inline 변수, voice preview placeholder)
 *  Step 3: 성격 · 취미 · 말투 (.chip-section, 각 최대 3개)
 *  Step 4: SUMMARY 카드 (보정② 나이 카피 제거 + 보정⑤ 라벨 ' · ' 으로 연결)
 */
import { createSoulmate, generatePortrait as __generatePortrait } from '../../api.js';
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

  // Day 7 Step 8 — Step 2 진입 시 Step 1 의 성별 선택과 일치하지 않는 카드 hidden 처리.
  // 커스텀 카드 (data-image-id="custom") 는 성별 무관 — 항상 보임.
  if (step === 2) applyGenderFilter();

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
      // 커스텀 트랙 — characterImageUrl 은 미리보기 컨펌 후에야 박힘
      state.characterImageUrl = '';
      applyTheme('bright');
      setTimeout(() => {
        const ta = grid.querySelector('[data-custom-prompt]');
        if (ta) ta.focus({ preventScroll: true });
      }, 280);
    } else {
      state.characterImageUrl = card.getAttribute('data-image-url') || '';
      // 프리셋 트랙 — 이전 커스텀 prompt + 미리보기 모두 비우기
      state.customAppearancePrompt = '';
      const ta = grid.querySelector('[data-custom-prompt]');
      if (ta) ta.value = '';
      hideCustomPreview();
      const theme = card.getAttribute('data-theme');
      if (theme) applyTheme(theme);
    }
    renderStep2();
    syncNextBtn();
  });

  // 커스텀 prompt 입력 → state 갱신 + 미리보기가 떠있으면 새 prompt 입력 시 *컨펌 무효화*
  const promptTextarea = grid.querySelector('[data-custom-prompt]');
  if (promptTextarea) {
    promptTextarea.addEventListener('input', () => {
      state.customAppearancePrompt = promptTextarea.value;
      // 사용자가 prompt 를 새로 입력하면 — 이전 미리보기 컨펌 무효화 (다시 생성 받게)
      if (state.characterImageUrl) {
        state.characterImageUrl = '';
        hideCustomPreview();
      }
      const promptCount = grid.querySelector('[data-prompt-count]');
      if (promptCount) {
        const len = state.customAppearancePrompt.length;
        promptCount.textContent = `${len} / 200`;
        promptCount.dataset.nearLimit = len > 160 ? 'true' : 'false';
      }
      syncNextBtn();
    });
  }

  // Day 7 Step 8 — 미리보기 컨펌/재시도 버튼
  const confirmBtn = grid.querySelector('[data-app-preview-confirm]');
  const retryBtn = grid.querySelector('[data-app-preview-retry]');
  if (confirmBtn) {
    confirmBtn.addEventListener('click', () => confirmCustomPortrait());
  }
  if (retryBtn) {
    retryBtn.addEventListener('click', () => retryCustomPortrait());
  }
}

/** 현재 step 의 다음 버튼 disabled 동기화 — bindStep2Handlers 의 textarea 입력 등에서 호출. */
function syncNextBtn() {
  const nextBtn = $(SELECTORS.nextBtn, rootEl);
  if (nextBtn) nextBtn.disabled = !canProceed(state.step);
  const submitBtn = $(SELECTORS.submitBtn, rootEl);
  if (submitBtn && state.step === 4) submitBtn.disabled = !canProceed(4);
}

/**
 * Day 7 Step 8 — Step 2 진입 시 Step 1 의 state.gender 와 일치하지 않는 카드 hidden 처리.
 * 커스텀 카드는 성별 무관 — 항상 보임.
 *
 * 또한 *현재 선택된 카드가 hidden 으로 갈리는* 경우 (예: Step 1 에서 성별 변경) 선택 해제.
 */
function applyGenderFilter() {
  const grid = $(SELECTORS.imageGrid, rootEl);
  if (!grid || !state.gender) return;
  const cards = grid.querySelectorAll('[data-app-card]');
  cards.forEach((card) => {
    const cardGender = card.getAttribute('data-gender');
    const id = card.getAttribute('data-image-id');
    // 커스텀 카드 (data-gender 없음) 는 항상 보임
    const shouldHide = cardGender && cardGender !== state.gender;
    card.classList.toggle('app-card--hidden', shouldHide);
    // 현재 선택된 카드가 숨겨지면 선택 해제
    if (shouldHide && state.characterImageId === id) {
      state.characterImageId = '';
      state.characterImageUrl = '';
      state.customAppearancePrompt = '';
      hideCustomPreview();
    }
  });
  renderStep2();
}

/**
 * Day 7 Step 8 — 커스텀 트랙의 *미리보기 + 컨펌* 흐름 핸들러.
 *
 * 흐름:
 *   1. Step 2 다음 버튼 클릭 (커스텀 트랙) → POST /api/images/portraits 호출 + spinner 노출
 *   2. 응답 도착 → spinner 숨김, .app-preview 펼침 (생성 이미지 + OK/다시 시도)
 *   3. *이 모습으로* 클릭 → state.characterImageUrl 박힘, Step 3 진입
 *   4. *다른 모습으로* 클릭 → 미리보기 숨김, textarea 활성화, 새 prompt 입력 가능
 *
 * 미리보기가 *이미 떠 있는 상태* 에서 다음 버튼이 다시 클릭되면 — 이미 컨펌 흐름이라 *그대로 Step 3 진입*.
 */
async function triggerCustomPortraitGeneration() {
  const promptBox = rootEl.querySelector('[data-app-prompt]');
  const previewBox = rootEl.querySelector('[data-app-preview]');
  if (!promptBox || !previewBox) return false;

  // 이미 컨펌된 상태 (characterImageUrl 박힘) — 그대로 다음 진입
  if (state.characterImageUrl) return true;

  const prompt = state.customAppearancePrompt.trim();
  if (prompt.length < 4) return false;

  // 1. spinner 활성화 + 다음 버튼 비활성화
  promptBox.dataset.loading = 'true';
  const nextBtn = $(SELECTORS.nextBtn, rootEl);
  if (nextBtn) {
    nextBtn.disabled = true;
    const span = nextBtn.querySelector('span');
    if (span) span.textContent = '얼굴 빚는 중…';
  }

  try {
    // generatePortrait API import 는 module 상단에서 추가됨
    const result = await __generatePortrait(prompt);

    // 2. 미리보기 표시
    state.characterImageUrl = result.localPath;
    promptBox.dataset.loading = 'false';
    const photo = previewBox.querySelector('[data-app-preview-photo]');
    if (photo) photo.style.setProperty('--preview-image', `url('${result.localPath}')`);
    previewBox.hidden = false;

    // 다음 버튼 텍스트 복원 (다음 클릭 시 Step 3 진입)
    if (nextBtn) {
      const span = nextBtn.querySelector('span');
      if (span) span.textContent = '다음';
      nextBtn.disabled = false;
    }
    return false; // 미리보기 노출 — 컨펌 후에 Step 3
  } catch (err) {
    promptBox.dataset.loading = 'false';
    if (nextBtn) {
      const span = nextBtn.querySelector('span');
      if (span) span.textContent = '다음';
      nextBtn.disabled = !canProceed(2);
    }
    alert(err.message || '이미지 생성에 실패했어요. 다시 시도해주세요.');
    return false;
  }
}

function hideCustomPreview() {
  const previewBox = rootEl ? rootEl.querySelector('[data-app-preview]') : null;
  if (previewBox) previewBox.hidden = true;
}

/** 미리보기 이후 *이 모습으로* 컨펌 — Step 3 으로 직접 진입한다 (showNextStep 의 기존 로직 재사용). */
function confirmCustomPortrait() {
  hideCustomPreview();
  // 다음 버튼의 click 이벤트가 *이미 컨펌된 상태* 임을 확인하고 Step 3 으로 진입
  const nextBtn = $(SELECTORS.nextBtn, rootEl);
  if (nextBtn) nextBtn.click();
}

/** 미리보기 이후 *다른 모습으로* — 다시 prompt 입력 받음. characterImageUrl 비우기 + 미리보기 숨김. */
function retryCustomPortrait() {
  state.characterImageUrl = '';
  hideCustomPreview();
  const promptTextarea = rootEl.querySelector('[data-custom-prompt]');
  if (promptTextarea) promptTextarea.focus({ preventScroll: true });
  syncNextBtn();
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
    nextBtn.addEventListener('click', async () => {
      if (state.step === 1) {
        showStep(2);
        renderStep2();
      } else if (state.step === 2) {
        // Day 7 Step 8 — 커스텀 트랙이고 *아직 미리보기 컨펌 전* 이면 이미지 생성 trigger.
        // 미리보기 노출 후 사용자가 *이 모습으로* 컨펌해야 비로소 Step 3 진입.
        if (state.characterImageId === 'custom' && !state.characterImageUrl) {
          await triggerCustomPortraitGeneration();
          return;
        }
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
