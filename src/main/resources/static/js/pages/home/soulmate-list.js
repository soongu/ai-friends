/**
 * Soulmate gallery — 미연시 .smcard 마크업 (테마 + 호감도 게이지 + 보이스 미리듣기 ▶)
 *
 *  카드 컨테이너(gallery)에는 정적 자식(intro, new 버튼, max-hint)이 미리 박혀 있다.
 *  카드들은 intro 다음에 동적으로 삽입한다.
 */
import { CONFIG, pickVoice } from '../../config.js';
import { getCharacterDef } from '../soulmate-create/options.js';
import { synthesizeSpeech, deleteSoulmate } from '../../api.js';

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

  // 3) 카드 클릭 → 채팅 페이지, voice preview ▶ 와 삭제 ✕ 는 별도 핸들러
  cardsEl.querySelectorAll('.smcard').forEach((card) => {
    card.addEventListener('click', (e) => {
      if (e.target.closest('.smcard__voice')) return;
      if (e.target.closest('.smcard__delete')) return;
      const id = card.dataset.soulmateId;
      if (id) window.location.href = CONFIG.routes.chat(id);
    });
  });
  cardsEl.querySelectorAll('[data-soulmate-voice-preview]').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const card = btn.closest('.smcard');
      const id = card?.dataset.soulmateId;
      const target = list.find((s) => String(s.id) === id);
      if (!target) return;
      playVoicePreview(card, btn, target).catch((err) => {
        console.warn('[voice] preview failed:', err?.message || err);
        alert(err?.message || '미리듣기에 실패했어요');
      });
    });
  });
  cardsEl.querySelectorAll('[data-soulmate-delete]').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const card = btn.closest('.smcard');
      const id = card?.dataset.soulmateId;
      const target = list.find((s) => String(s.id) === id);
      if (!card || !id || !target) return;
      handleDelete(container, card, btn, target).catch((err) => {
        console.warn('[delete] failed:', err?.message || err);
        alert(err?.message || '삭제에 실패했어요');
      });
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
      <button type="button" class="smcard__delete"
              data-soulmate-delete
              aria-label="${escapeAttr(s.name)} 삭제">
        <svg viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <path d="M3 3L11 11M11 3L3 11"/>
        </svg>
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

// ============================================================
// 보이스 미리듣기 — 캐릭터 인사말 한 줄을 TTS 로 합성해 재생
//   디자인 명세: .smcard--voice-playing + .smcard__voice--playing
//                + .smcard__notes(♪♬♪) + .smcard__progress (--progress 갱신)
// ============================================================
let currentPreview = null; // { audio, card, btn, notes, progress, raf } — 재생 중인 미리듣기

function buildPreviewLine(s) {
  const name = s.name || '소울메이트';
  const def = getCharacterDef(s.characterImageId);
  // 캐릭터 분위기에 맞춰 한 줄 인사말 — TTS 비용 억제 위해 짧게.
  return `안녕, 나는 ${name}이야. ${def?.mood || ''}에서 너를 기다리고 있어.`;
}

async function playVoicePreview(card, btn, soulmate) {
  // 같은 카드 재클릭 → 정지
  if (currentPreview && currentPreview.card === card) {
    stopCurrentPreview();
    return;
  }
  // 다른 카드 재생 중이면 끊고 교체
  stopCurrentPreview();

  btn.classList.add('smcard__voice--loading');
  card.classList.add('smcard--voice-hover'); // 로딩 동안 mist 글로우
  try {
    const { blob } = await synthesizeSpeech(buildPreviewLine(soulmate), pickVoice(soulmate.characterImageId));
    const url = URL.createObjectURL(blob);
    const audio = new Audio(url);

    // 디자인 명세 — playing 상태로 전환
    card.classList.remove('smcard--voice-hover');
    card.classList.add('smcard--voice-playing');
    btn.classList.add('smcard__voice--playing');
    setPlayIcon(btn, true);

    // 음표 파티클 ♪♬♪ — playing 시 카드에 박힘
    const notes = document.createElement('div');
    notes.className = 'smcard__notes';
    notes.setAttribute('aria-hidden', 'true');
    notes.innerHTML = '<span>♪</span><span>♬</span><span>♪</span>';
    card.appendChild(notes);

    // progress 라인 — audio.currentTime / duration 으로 갱신
    const progress = document.createElement('div');
    progress.className = 'smcard__progress';
    progress.style.setProperty('--progress', '0%');
    card.appendChild(progress);

    const session = { audio, card, btn, notes, progress, raf: 0, url };
    currentPreview = session;

    const tick = () => {
      if (currentPreview !== session) return;
      const dur = audio.duration || 0;
      if (dur > 0) {
        const pct = Math.min(100, Math.max(0, (audio.currentTime / dur) * 100));
        progress.style.setProperty('--progress', `${pct}%`);
      }
      session.raf = requestAnimationFrame(tick);
    };
    session.raf = requestAnimationFrame(tick);

    audio.addEventListener('ended', () => {
      if (currentPreview === session) stopCurrentPreview();
    });
    audio.addEventListener('error', () => {
      if (currentPreview === session) stopCurrentPreview();
    });

    await audio.play();
  } finally {
    btn.classList.remove('smcard__voice--loading');
  }
}

function stopCurrentPreview() {
  if (!currentPreview) return;
  const { audio, card, btn, notes, progress, raf, url } = currentPreview;
  try { audio.pause(); } catch (_) { /* noop */ }
  if (raf) cancelAnimationFrame(raf);
  if (url) URL.revokeObjectURL(url);
  card.classList.remove('smcard--voice-playing', 'smcard--voice-hover');
  if (btn) {
    btn.classList.remove('smcard__voice--playing');
    setPlayIcon(btn, false);
  }
  if (notes) notes.remove();
  if (progress) progress.remove();
  currentPreview = null;
}

// 디자인 명세 — playing 시 ▶ 아이콘이 ‖ (pause) 로 바뀐다
const PLAY_SVG = '<svg viewBox="0 0 14 14" fill="currentColor"><path d="M3.5 2.5L11.5 7L3.5 11.5V2.5Z"/></svg>';
const PAUSE_SVG = '<svg viewBox="0 0 14 14" fill="currentColor"><rect x="3" y="2.5" width="2.6" height="9" rx="0.6"/><rect x="8.4" y="2.5" width="2.6" height="9" rx="0.6"/></svg>';
function setPlayIcon(btn, playing) {
  btn.innerHTML = playing ? PAUSE_SVG : PLAY_SVG;
  btn.setAttribute('aria-label', playing ? '미리듣기 일시정지' : '캐릭터 보이스 미리듣기');
}

// ============================================================
// 삭제 — 인증 없는 데모용 하드 삭제 (서버에서 대화/뱃지까지 캐스케이드)
//   1) confirm 으로 안전판
//   2) voice preview 가 이 카드에서 재생 중이면 정지
//   3) DELETE 호출 → 카드 페이드아웃 → 페이지 reload
//      reload 하는 이유: 마지막 캐릭터 삭제 시 home 진입 로직(landing vs list) 이 다시 분기되도록.
// ============================================================
async function handleDelete(container, card, btn, soulmate) {
  const name = soulmate.name || '소울메이트';
  if (!window.confirm(`정말 ${name} 캐릭터를 삭제할까요?\n대화 기록과 뱃지도 함께 사라지고 되돌릴 수 없어요.`)) {
    return;
  }

  // voice preview 가 이 카드에서 재생 중이면 정지
  if (currentPreview && currentPreview.card === card) {
    stopCurrentPreview();
  }

  btn.disabled = true;
  card.classList.add('smcard--deleting');
  try {
    await deleteSoulmate(Number(soulmate.id));
    // 페이드아웃을 잠깐 보여주고 reload (home 의 진입 분기를 다시 태운다)
    setTimeout(() => window.location.reload(), 280);
  } catch (err) {
    btn.disabled = false;
    card.classList.remove('smcard--deleting');
    throw err;
  }
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
