/**
 * 메인(홈) 페이지 진입점 — 뷰 전환, 초기화
 * 서버에 캐릭터 목록 조회 후 있으면 캐릭터 선택 화면, 없으면 랜딩(시작하기 → 캐릭터 생성)
 */
import { CONFIG } from '../../config.js';
import { getSoulmates } from '../../api.js';
import { initLanding, renderLandingLogo } from './landing.js';
import { renderSoulmateList, initSoulmateListLogo } from './soulmate-list.js';

const SELECTORS = {
  appRoot: '[data-app-root]',
  viewLanding: '[data-view="landing"]',
  viewSoulmateList: '[data-view="soulmate-list"]',
};

/**
 * API 소울메이트 항목을 목록 뷰용 형태로 변환
 * @param {{ id: number, name?: string, personalityKeywords?: string, hobbies?: string, characterImageUrl?: string }} s
 * @returns {{ id: number, name: string, meta: string, characterImageUrl?: string }}
 */
function toListItem(s) {
  const name = s.name || '소울메이트';
  const parts = [s.personalityKeywords, s.hobbies].filter(Boolean);
  const meta = parts.length ? parts.join(' · ') : '';
  return { id: s.id, name, meta, characterImageUrl: s.characterImageUrl };
}

/**
 * 서버에 캐릭터 목록 조회 후 상태 결정: 있으면 캐릭터 선택, 없으면 랜딩
 */
async function getInitialState() {
  try {
    const list = await getSoulmates();
    if (list && list.length > 0) {
      return { view: 'soulmate-list', soulmates: list.map(toListItem) };
    }
  } catch (_) {
    // API 실패 시 랜딩으로 폴백
  }
  return { view: 'landing', soulmates: [] };
}

function showView(viewName) {
  const root = document.querySelector(SELECTORS.appRoot);
  if (!root) return;
  const mainContainer = root.querySelector('.main-container');
  if (!mainContainer) return;
  mainContainer.classList.toggle(
    'view-soulmate-list',
    viewName === 'soulmate-list',
  );
}

async function main() {
  const root = document.querySelector(SELECTORS.appRoot);
  if (!root) return;

  const state = await getInitialState();

  showView(state.view);

  if (state.view === 'landing') {
    initLanding(root);
    renderLandingLogo(root);
  } else {
    renderSoulmateList(
      root.querySelector(SELECTORS.viewSoulmateList),
      state.soulmates,
    );
    initSoulmateListLogo(root.querySelector(SELECTORS.viewSoulmateList));
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', main);
} else {
  main();
}
