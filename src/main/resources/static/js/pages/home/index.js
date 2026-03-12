/**
 * 메인(홈) 페이지 진입점 — 뷰 전환, 초기화
 * ES module 방식으로 React 전환 시 라우터/App으로 대체 가능
 */
import { CONFIG } from '../../config.js';
import { initLanding, renderLandingLogo } from './landing.js';
import { renderSoulmateList, initSoulmateListLogo } from './soulmate-list.js';

const SELECTORS = {
  appRoot: '[data-app-root]',
  viewLanding: '[data-view="landing"]',
  viewSoulmateList: '[data-view="soulmate-list"]',
};

/**
 * 상태: 'landing' | 'soulmate-list'
 * 실제 연동 시 서버에서 내려주는 값 또는 API로 판단
 */
function getInitialState() {
  // 프로토타입: URL 쿼리 ?soulmates=1 이면 선택 화면 표시 (목 데이터)
  const params = new URLSearchParams(window.location.search);
  if (params.get('soulmates') === '1') {
    return {
      view: 'soulmate-list',
      soulmates: [
        { id: '1', name: '앨리스', meta: '친절한 · 유머러스' },
        { id: '2', name: '죠니뎁', meta: '차분한 · 감성적' },
      ],
    };
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

function main() {
  const state = getInitialState();
  const root = document.querySelector(SELECTORS.appRoot);
  if (!root) return;

  showView(state.view);

  if (state.view === 'landing') {
    initLanding(root);
    renderLandingLogo(root);
  } else {
    renderSoulmateList(root.querySelector(SELECTORS.viewSoulmateList), state.soulmates);
    initSoulmateListLogo(root.querySelector(SELECTORS.viewSoulmateList));
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', main);
} else {
  main();
}
