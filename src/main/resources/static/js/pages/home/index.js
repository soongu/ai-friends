/**
 * 메인(홈) 페이지 진입점 — 뷰 전환, 초기화
 *
 *  서버에 캐릭터 목록 조회 → 있으면 갤러리 뷰(.smcard 들), 없으면 랜딩(시작하기 → 캐릭터 생성).
 *  뷰 전환은 layout.css 의 `.main-container.view-soulmate-list [data-view="..."]` CSS 분기로.
 */
import { getSoulmates } from '../../api.js';
import { initLanding } from './landing.js';
import { renderSoulmateList } from './soulmate-list.js';

const SELECTORS = {
  appRoot: '[data-app-root]',
  viewLanding: '[data-view="landing"]',
  viewSoulmateList: '[data-view="soulmate-list"]',
};

/**
 * API 소울메이트 항목을 갤러리 뷰용 형태로 변환.
 * .smcard 마크업이 themeKey · 호감도 · 레벨까지 그리려면 character/level/score 도 함께 넘겨야 한다.
 */
function toListItem(s) {
  return {
    id: s.id,
    name: s.name || '소울메이트',
    characterImageId: s.characterImageId,
    characterImageUrl: s.characterImageUrl,
    affectionScore: s.affectionScore ?? 0,
    level: s.level ?? 1,
  };
}

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
  mainContainer.classList.toggle('view-soulmate-list', viewName === 'soulmate-list');
}

async function main() {
  const root = document.querySelector(SELECTORS.appRoot);
  if (!root) return;

  const state = await getInitialState();

  showView(state.view);

  if (state.view === 'landing') {
    initLanding(root.querySelector(SELECTORS.viewLanding));
  } else {
    renderSoulmateList(
      root.querySelector(SELECTORS.viewSoulmateList),
      state.soulmates,
    );
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', main);
} else {
  main();
}
