import * as Speech from 'expo-speech';
import { Platform } from 'react-native';

const isWeb = Platform.OS === 'web';

// ============ TTS 语音播报（原生 App / 桌面浏览器）============
// 手机 H5 上 speechSynthesis 常被浏览器静音拦截（iOS Safari / Android Chrome），
// 因此另有 ding() 提示音兜底，保证「收款到账」在手机 H5 也有声音提醒。

export function speak(text) {
  if (!text) return;
  try {
    Speech.stop();
    Speech.speak(text, { language: 'zh-CN', rate: 1 });
  } catch (e) {
    // 无 TTS 引擎 / 当前环境不支持时静默失败，不影响主流程
  }
}

export function stopSpeaking() {
  try { Speech.stop(); } catch (e) {}
}

// ============ 「叮咚」提示音（Web Audio，手机 H5 也能响）============

let audioCtx = null;
let warmed = false;

function ensureAudioContext() {
  if (!isWeb || typeof window === 'undefined') return null;
  try {
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return null;
    if (!audioCtx) audioCtx = new AC();
    if (audioCtx.state === 'suspended') audioCtx.resume();
    return audioCtx;
  } catch (e) {
    return null;
  }
}

// 手机浏览器限制：必须先有一次用户手势，才允许自动播放声音。
// 这里在首次 touch/click 时「解锁」AudioContext（商户点一下登录即可解锁）。
if (isWeb && typeof window !== 'undefined') {
  const unlock = () => {
    const ctx = ensureAudioContext();
    if (!ctx || warmed) return;
    warmed = true;
    try {
      // 用一段极短静音预热，确保后续能正常出声
      const buf = ctx.createBuffer(1, 1, 22050);
      const src = ctx.createBufferSource();
      src.buffer = buf;
      src.connect(ctx.destination);
      src.start(0);
    } catch (e) {}
  };
  ['touchstart', 'touchend', 'click', 'keydown'].forEach((evt) =>
    window.addEventListener(evt, unlock)
  );
}

// 播一段「叮~咚」双音提示
export function ding() {
  if (!isWeb) return;
  const ctx = ensureAudioContext();
  if (!ctx) return;
  try {
    const now = ctx.currentTime;
    const notes = [
      [880.0, 0.0],     // 叮（A5）
      [1174.66, 0.18],  // 咚（D6）
    ];
    notes.forEach(([freq, offset]) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.value = freq;

      const t = now + offset;
      gain.gain.setValueAtTime(0.0001, t);
      gain.gain.exponentialRampToValueAtTime(0.4, t + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.5);

      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(t);
      osc.stop(t + 0.55);
    });
  } catch (e) {}
}
