import { useEffect, useRef } from 'react';
import { useAuth } from '../store/AuthContext';
import { getOrderList } from '../api/order';
import { speak, ding } from '../utils/voice';

// 收款到账语音播报（前台轮询方案）：
// 登录后每 POLL_INTERVAL 毫秒轮询一次「已支付」订单（orderStatus=2），
// 按 orderNo 去重，仅播报「新」到账订单，避免进入 App 时把历史订单全部重播一遍。

const POLL_INTERVAL = 5000; // 轮询间隔（毫秒）
const POLL_SIZE = 20;       // 每次取最近 N 条已支付订单

function buildText(order) {
  const channel = order.payChannel === 'WECHAT' ? '微信'
    : order.payChannel === 'ALIPAY' ? '支付宝' : '';
  const amount = Number(order.orderAmount || 0).toFixed(2);
  return `${channel}收款到账 ${amount} 元`;
}

// 渲染 null，仅作为全局副作用组件挂载在 AuthProvider 内
export default function PaymentVoiceNotifier() {
  const { user } = useAuth();
  const seenRef = useRef(new Set());
  const initedRef = useRef(false);

  useEffect(() => {
    if (!user) {
      // 登出后重置，避免下次登录把旧订单当作新订单
      initedRef.current = false;
      seenRef.current.clear();
      return;
    }

    let cancelled = false;
    let timer = null;

    const poll = async () => {
      if (cancelled) return;
      try {
        const result = await getOrderList({ page: 1, size: POLL_SIZE, orderStatus: 2 });
        const records = result?.records || [];
        for (const order of records) {
          const key = order.orderNo || String(order.id);
          if (seenRef.current.has(key)) continue;
          seenRef.current.add(key);
          // 首次轮询只做「基线」，不重播历史订单
          if (initedRef.current) {
            // 叮咚提示音（手机 H5 也能响）+ TTS 播报金额（桌面/原生端）
            ding();
            speak(buildText(order));
          }
        }
        initedRef.current = true;
      } catch (e) {
        // 网络失败静默，下个周期重试
      }
      if (!cancelled) timer = setTimeout(poll, POLL_INTERVAL);
    };

    poll();
    return () => {
      cancelled = true;
      if (timer) clearTimeout(timer);
    };
  }, [user]);

  return null;
}
