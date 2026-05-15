/**
 * 채팅 페이지 BGM 컨트롤러
 *
 *  - /chat 진입 시 자동 재생 시도. 브라우저의 autoplay 정책으로 막히면
 *    첫 사용자 제스처(클릭/키다운/터치) 한 번에 재생 시작.
 *  - TTS 재생 동안 볼륨 ducking (50% → 12%) 으로 끊김 없이 은은하게 깔린다.
 *  - 우측 상단 햄버거 왼쪽의 mute 버튼으로 토글. 상태는 localStorage 박제.
 *  - 재생/뮤트 상태에 따라 4-bar equalizer 애니메이션 + slash overlay 로 시각 표현.
 */

const STORAGE_KEY = 'aifriends.bgm.muted';
const BASE_VOLUME = 0.45;     // 기본 BGM 볼륨
const DUCKED_VOLUME = 0.12;   // TTS 재생 중 ducked 볼륨
const FADE_MS = 480;          // ducking 페이드 시간

let audioEl = null;
let toggleBtn = null;
let muted = false;
let ttsActive = false;
let fadeTimer = null;
let unlockHandler = null;
let audioContext = null;
let pausedForRecording = false;

function readMuted() {
  return localStorage.getItem(STORAGE_KEY) === 'true';
}
function writeMuted(v) {
  localStorage.setItem(STORAGE_KEY, v ? 'true' : 'false');
}

function targetVolume() {
  if (muted) return 0;
  return ttsActive ? DUCKED_VOLUME : BASE_VOLUME;
}

// 부드러운 볼륨 전환 — 페이드 인/아웃
function fadeTo(target, duration = FADE_MS) {
  if (!audioEl) return;
  if (fadeTimer) {
    clearInterval(fadeTimer);
    fadeTimer = null;
  }
  const start = audioEl.volume;
  const delta = target - start;
  if (Math.abs(delta) < 0.005) {
    audioEl.volume = target;
    return;
  }
  const steps = Math.max(1, Math.round(duration / 30));
  let i = 0;
  fadeTimer = setInterval(() => {
    i += 1;
    const p = i / steps;
    const eased = p < 0.5 ? 2 * p * p : 1 - Math.pow(-2 * p + 2, 2) / 2; // ease-in-out
    audioEl.volume = Math.max(0, Math.min(1, start + delta * eased));
    if (i >= steps) {
      clearInterval(fadeTimer);
      fadeTimer = null;
      audioEl.volume = target;
    }
  }, 30);
}

function applyVisualState() {
  if (!toggleBtn) return;
  toggleBtn.classList.toggle('bgm-toggle--muted', muted);
  toggleBtn.classList.toggle('bgm-toggle--playing', !muted && !audioEl?.paused);
  toggleBtn.setAttribute('aria-pressed', muted ? 'true' : 'false');
  toggleBtn.setAttribute('aria-label', muted ? '배경음 켜기' : '배경음 끄기');
}

async function tryPlay() {
  if (!audioEl || muted) return false;
  try {
    audioEl.volume = 0; // 0에서 페이드인
    await audioEl.play();
    fadeTo(targetVolume(), 800);
    applyVisualState();
    return true;
  } catch (_) {
    return false;
  }
}

function attachUnlockListeners() {
  if (unlockHandler) return;
  unlockHandler = async () => {
    const ok = await tryPlay();
    if (ok) detachUnlockListeners();
  };
  ['pointerdown', 'click', 'keydown', 'touchstart'].forEach((ev) => {
    document.addEventListener(ev, unlockHandler, { once: false, passive: true });
  });
}
function detachUnlockListeners() {
  if (!unlockHandler) return;
  ['pointerdown', 'click', 'keydown', 'touchstart'].forEach((ev) => {
    document.removeEventListener(ev, unlockHandler);
  });
  unlockHandler = null;
}

async function toggleMute() {
  muted = !muted;
  writeMuted(muted);
  if (muted) {
    fadeTo(0, 240);
    applyVisualState();
    // 완전히 페이드아웃 된 뒤 pause — 재개 시 끊김 없게 currentTime 유지
    setTimeout(() => {
      if (muted && audioEl) audioEl.pause();
    }, 280);
  } else {
    if (audioEl?.paused) {
      const ok = await tryPlay();
      if (!ok) attachUnlockListeners();
    } else {
      fadeTo(targetVolume());
    }
    applyVisualState();
  }
}

/**
 * TTS 가 재생 중인 동안 BGM 볼륨을 줄였다가, 끝나면 복원.
 * chat/index.js 의 setTtsPlaying() 에서 호출.
 */
export function setTtsActive(active) {
  ttsActive = !!active;
  if (!audioEl || muted || pausedForRecording) return;
  fadeTo(targetVolume());
}

// ============================================================
// STT 시작 시퀀스 — BGM 페이드아웃 → 띠링 효과음 → 녹음 시작
// ============================================================

function ensureAudioContext() {
  if (!audioContext) {
    const Ctor = window.AudioContext || window.webkitAudioContext;
    if (Ctor) audioContext = new Ctor();
  }
  if (audioContext && audioContext.state === 'suspended') {
    audioContext.resume().catch(() => {});
  }
  return audioContext;
}

/**
 * 두 음짜리 ascending chime — "띠~링". sine + 짧은 ADSR 로 부드러운 벨 톤.
 * Web Audio 로 즉석 합성하므로 효과음 파일 불필요.
 * @returns {number} chime 총 길이 (ms)
 */
export function playStartChime() {
  const ctx = ensureAudioContext();
  if (!ctx) return 0;

  const now = ctx.currentTime;
  const notes = [
    { freq: 880,    start: 0.00, dur: 0.16 }, // A5
    { freq: 1318.5, start: 0.09, dur: 0.30 }, // E6 (한 옥타브 + 5도 위)
  ];

  notes.forEach((n) => {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.value = n.freq;

    gain.gain.setValueAtTime(0, now + n.start);
    gain.gain.linearRampToValueAtTime(0.22, now + n.start + 0.012);
    gain.gain.exponentialRampToValueAtTime(0.001, now + n.start + n.dur);

    osc.connect(gain).connect(ctx.destination);
    osc.start(now + n.start);
    osc.stop(now + n.start + n.dur + 0.05);
  });

  return 400; // 0.09 + 0.30 + 약간의 여유 = 약 400ms
}

/**
 * 녹음 직전 호출 — BGM 페이드아웃 후 pause. 마이크가 BGM 을 픽업하지 않게.
 */
export function pauseBgmForRecording() {
  pausedForRecording = true;
  if (!audioEl || muted) return;
  fadeTo(0, 220);
  setTimeout(() => {
    if (pausedForRecording && audioEl && !audioEl.paused) {
      try { audioEl.pause(); } catch (_) { /* noop */ }
    }
  }, 240);
}

/**
 * 녹음 종료/취소 직후 호출 — BGM 재개 + 페이드인. (TTS 활성 시 ducked 볼륨으로)
 */
export function resumeBgmAfterRecording() {
  pausedForRecording = false;
  if (!audioEl || muted) return;
  if (audioEl.paused) {
    audioEl.play().catch(() => {});
  }
  fadeTo(targetVolume(), 600);
}

export function initBgm() {
  audioEl = document.querySelector('[data-bgm]');
  toggleBtn = document.querySelector('[data-bgm-toggle]');
  if (!audioEl || !toggleBtn) return;

  muted = readMuted();
  audioEl.loop = true;
  audioEl.volume = 0;

  toggleBtn.addEventListener('click', toggleMute);

  audioEl.addEventListener('playing', applyVisualState);
  audioEl.addEventListener('pause', applyVisualState);
  audioEl.addEventListener('ended', applyVisualState);

  applyVisualState();

  if (muted) {
    return; // 음소거 상태로 진입했으면 재생 시도 안 함
  }

  // 1. 즉시 재생 시도 (autoplay 정책 통과하는 일부 케이스)
  tryPlay().then((ok) => {
    if (!ok) {
      // 2. 막혔으면 첫 제스처에 재생 시작
      attachUnlockListeners();
    }
  });
}
