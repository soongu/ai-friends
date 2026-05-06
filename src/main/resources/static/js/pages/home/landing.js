/**
 * Landing 뷰 — Start CTA 핸들러.
 *  미연시 디자인엔 별도 로고 placeholder 가 없으므로 logo 관련 함수는 no-op.
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

/** 호환 유지 — 새 디자인은 로고 자체가 없다 */
export function renderLandingLogo() {}
