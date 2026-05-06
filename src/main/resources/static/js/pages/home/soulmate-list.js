/**
 * Soulmate gallery — 미연시 .smcard 마크업 (테마 + 호감도 게이지 + 보이스 미리듣기 ▶)
 *
 *  카드 컨테이너(gallery)에는 정적 자식(intro, new 버튼, max-hint)이 미리 박혀 있다.
 *  카드들은 intro 다음에 동적으로 삽입한다.
 */
import { CONFIG } from '../../config.js';
import { getCharacterDef } from '../soulmate-create/options.js';

const SELECTORS = {
  cards: '[data-soulmate-cards]',
  newBtn: '[data-soulmate-new-btn]',
  newSub: '[data-soulmate-new-sub]',
  countLabel: '[data-soulmate-count-label]',
  maxHint: '[data-soulmate-max-hint]',
};

const THEME_VARS = ['primary', 'secondary', 'accent', 'deep', 'mist', 'glow'];

/**
 * @param {HTMLElement} container
 * @param {{ id: number, name: string, characterImageId?: string, characterImageUrl?: string,
 *           affectionScore?: number, level?: number }[]} list
 */
export function renderSoulmateList(container, list = []) {
  if (!container) return;
  const cardsEl = container.querySelector(SELECTORS.cards);
  const newBtn = container.querySelector(SELECTORS.newBtn);
  const newSub = container.querySelector(SELECTORS.newSub);
  const countLabel = container.querySelector(SELECTORS.countLabel);
  const maxHint = container.querySelector(SELECTORS.maxHint);
  if (!cardsEl) return;

  const max = CONFIG.maxSoulmates;
  const count = list.length;
  const isMax = count >= max;

  // 1) 기존 동적 카드 제거 (intro / new 버튼 / max-hint 는 정적이라 보존)
  cardsEl.querySelectorAll('.smcard').forEach((c) => c.remove());

  // 2) 카드 HTML 일괄 생성 후 intro 다음에 삽입 (또는 컨테이너 시작)
  const html = list.map(renderCardHtml).join('');
  const introEl = cardsEl.querySelector('.gallery__intro');
  if (introEl) {
    introEl.insertAdjacentHTML('afterend', html);
  } else {
    cardsEl.insertAdjacentHTML('afterbegin', html);
  }

  // 3) 카드 클릭 → 채팅 페이지, voice preview ▶ 는 별도 핸들러
  cardsEl.querySelectorAll('.smcard').forEach((card) => {
    card.addEventListener('click', (e) => {
      if (e.target.closest('.smcard__voice')) return;
      const id = card.dataset.soulmateId;
      if (id) window.location.href = CONFIG.routes.chat(id);
    });
  });
  cardsEl.querySelectorAll('[data-soulmate-voice-preview]').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      // Day 9 에서 활성화 — 미리듣기 오디오 fetch + 카드 ring-glow 토글
      console.info('[voice] character preview placeholder — Day 9 에서 활성화 예정');
    });
  });

  // 4) 카운트 라벨 갱신
  if (countLabel) countLabel.textContent = `YOUR SOULMATES · ${count} / ${max}`;
  if (newSub)     newSub.textContent     = `Create New · ${count} / ${max}`;

  // 5) new 버튼 / max-hint 가시성
  if (newBtn) {
    newBtn.disabled = isMax;
    newBtn.hidden = isMax;
    if (!newBtn.dataset.bound) {
      newBtn.dataset.bound = 'true';
      newBtn.addEventListener('click', () => {
        if (!newBtn.disabled) window.location.href = CONFIG.routes.newSoulmate;
      });
    }
  }
  if (maxHint) maxHint.hidden = !isMax;
}

function renderCardHtml(s) {
  const def = getCharacterDef(s.characterImageId);
  const themeKey = def.themeKey;
  const thumbUrl =
    s.characterImageUrl ||
    `/images/characters/${s.characterImageId || 'character-female-bright'}-thumb.jpg`;
  const gauge = Math.max(0, Math.min(100, (s.affectionScore ?? 0) % 100));
  const themeStyle = THEME_VARS
    .map((v) => `--theme-${v}: var(--theme-${themeKey}-${v});`)
    .join(' ');
  const inlineStyle = `${themeStyle} --thumb-image: url('${escapeAttr(thumbUrl)}');`;

  return `
    <div class="smcard"
         data-soulmate-id="${escapeAttr(String(s.id))}"
         data-soulmate-voice-id="${escapeAttr(s.characterImageId || '')}"
         style="${inlineStyle}">
      <div class="smcard__thumb" aria-hidden="true"></div>
      <div class="smcard__cut" aria-hidden="true"></div>
      <div class="smcard__motes" aria-hidden="true">
        <span></span><span></span><span></span><span></span>
      </div>
      <button type="button" class="smcard__voice"
              data-soulmate-voice-preview
              data-soulmate-voice-id="${escapeAttr(s.characterImageId || '')}"
              aria-label="캐릭터 보이스 미리듣기">
        <svg viewBox="0 0 14 14" fill="currentColor"><path d="M3.5 2.5L11.5 7L3.5 11.5V2.5Z"/></svg>
      </button>
      <div class="smcard__info">
        <div class="smcard__info-text">
          <div class="smcard__name">
            <span class="smcard__name-dot" aria-hidden="true"></span>
            ${escapeHtml(s.name)}
          </div>
          <div class="smcard__mood">${escapeHtml(def.mood)}</div>
        </div>
        <div class="smcard__affection">
          <svg class="heart" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M12 21s-7-4.5-9.5-9C.7 8.4 2.5 4 6.5 4c2 0 3.5 1 5.5 3.2C13.5 5 15.5 4 17.5 4c4 0 5.8 4.4 4 8-2.5 4.5-9.5 9-9.5 9z" fill="#ff5a8a" stroke="#fff" stroke-width="1.2"/>
          </svg>
          <div class="lv">Lv.${s.level ?? 1}</div>
          <div class="gauge" style="--gauge: ${gauge}%" aria-label="호감도 ${gauge}%"></div>
        </div>
      </div>
    </div>`;
}

/** 미연시 디자인엔 별도 로고가 없으므로 no-op (호환 유지) */
export function initSoulmateListLogo() {}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str ?? '';
  return div.innerHTML;
}
function escapeAttr(str) {
  if (!str) return '';
  return escapeHtml(str).replace(/"/g, '&quot;');
}
