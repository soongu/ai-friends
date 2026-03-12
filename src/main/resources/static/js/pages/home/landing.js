/**
 * Landing 뷰 — 시작하기 CTA, 네비게이션
 * React 전환 시 컴포넌트 내부로 이동
 */
import { CONFIG } from '../../config.js';

const SELECTORS = {
  cta: '[data-landing-cta]',
};

export function initLanding(container) {
  if (!container) return;
  const cta = container.querySelector(SELECTORS.cta);
  if (!cta) return;

  cta.addEventListener('click', () => {
    window.location.href = CONFIG.routes.start;
  });
}

export function renderLandingLogo(container) {
  if (!container) return;
  const el = container.querySelector('[data-landing-logo]');
  if (!el) return;
  if (CONFIG.logoImageUrl) {
    el.src = CONFIG.logoImageUrl;
    el.classList.remove('landing__logo--placeholder');
  }
  // CONFIG.copy.taglineLanding 반영
  const tagline = container.querySelector('[data-landing-tagline]');
  if (tagline && CONFIG.copy.taglineLanding) tagline.textContent = CONFIG.copy.taglineLanding;
}
