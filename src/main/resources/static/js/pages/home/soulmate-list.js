/**
 * Soulmate list 뷰 — 목록 렌더, 선택, 새로 만들기
 * React 전환 시 컴포넌트 + hooks로 이식
 */
import { CONFIG } from '../../config.js';

const SELECTORS = {
  cards: '[data-soulmate-cards]',
  newBtn: '[data-soulmate-new-btn]',
  maxHint: '[data-soulmate-max-hint]',
};

/**
 * @param {HTMLElement} container
 * @param {{ id: string, name: string, meta?: string, characterImageUrl?: string }[]} list
 */
export function renderSoulmateList(container, list = []) {
  if (!container) return;
  const cardsEl = container.querySelector(SELECTORS.cards);
  const newBtn = container.querySelector(SELECTORS.newBtn);
  const maxHint = container.querySelector(SELECTORS.maxHint);
  if (!cardsEl) return;

  const max = CONFIG.maxSoulmates;
  const isMax = list.length >= max;

  cardsEl.innerHTML = list
    .map(
      (s) => `
    <li class="soulmate-list__card" data-soulmate-id="${s.id}">
      <div class="soulmate-list__card-avatar" aria-hidden="true">
        ${s.characterImageUrl ? `<img src="${escapeAttr(s.characterImageUrl)}" alt="" />` : ''}
      </div>
      <div>
        <p class="soulmate-list__card-name">${escapeHtml(s.name)}</p>
        ${s.meta ? `<p class="soulmate-list__card-meta">${escapeHtml(s.meta)}</p>` : ''}
      </div>
    </li>
  `,
    )
    .join('');

  cardsEl.querySelectorAll('[data-soulmate-id]').forEach((card) => {
    card.addEventListener('click', () => {
      const id = card.getAttribute('data-soulmate-id');
      if (id) window.location.href = CONFIG.routes.chat(id);
    });
  });

  if (newBtn) {
    newBtn.disabled = isMax;
    newBtn.textContent = CONFIG.copy.ctaNew;
    newBtn.addEventListener('click', () => {
      if (!isMax) window.location.href = CONFIG.routes.newSoulmate;
    });
  }

  if (maxHint) {
    maxHint.textContent = CONFIG.copy.maxHint;
    maxHint.hidden = !isMax;
  }
}

export function initSoulmateListLogo(container) {
  if (!container) return;
  const el = container.querySelector('[data-soulmate-list-logo]');
  if (!el || !CONFIG.logoImageUrl) return;
  el.src = CONFIG.logoImageUrl;
  el.classList.remove('landing__logo--placeholder');
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
