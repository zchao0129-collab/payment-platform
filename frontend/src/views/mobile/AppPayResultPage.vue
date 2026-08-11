<template>
  <div class="result-page">
    <div class="result-content">
      <!-- Success -->
      <template v-if="status === 'success'">
        <div class="result-circle success">
          <span class="result-icon">✓</span>
        </div>
        <div class="result-title">支付成功</div>
        <div class="result-amount">¥{{ amount || '—' }}</div>
        <div class="info-list">
          <div class="info-item">
            <span class="info-label">订单号</span>
            <span class="info-value">{{ orderNo || '—' }}</span>
          </div>
        </div>
      </template>

      <!-- Failed / Cancelled -->
      <template v-else-if="status === 'failed' || status === 'TRADE_CLOSED'">
        <div class="result-circle fail">
          <span class="result-icon">✕</span>
        </div>
        <div class="result-title">支付未完成</div>
        <div class="result-desc">{{ statusText }}</div>
      </template>

      <!-- Unknown / Checking -->
      <template v-else>
        <div class="result-circle pending">
          <span class="result-icon">?</span>
        </div>
        <div class="result-title">支付结果确认中</div>
        <div class="result-desc">如已支付，请等待商户确认</div>
        <div class="info-list">
          <div class="info-item">
            <span class="info-label">订单号</span>
            <span class="info-value">{{ orderNo || '—' }}</span>
          </div>
        </div>
      </template>
    </div>

    <div class="action-area">
      <button class="done-button" @click="goBack">
        完 成
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderStatus } from '@/api/order'

const route = useRoute()
const router = useRouter()

const orderNo = ref('')
const amount = ref('')
const status = ref('unknown')

let pollTimer = null
const POLL_INTERVAL = 2000
const POLL_TIMEOUT = 60_000

const statusText = computed(() => {
  switch (status.value) {
    case 'success': return '支付成功'
    case 'failed': return '支付失败，请重试'
    case 'TRADE_CLOSED': return '交易已关闭'
    case 'TRADE_FINISHED': return '交易已完成'
    default: return '支付结果确认中'
  }
})

onMounted(() => {
  orderNo.value = route.query.orderNo || ''
  amount.value = route.query.amount || ''

  // Alipay sync return may carry trade_status — apply immediately if terminal
  const tradeStatus = route.query.status
  if (tradeStatus === 'success' || tradeStatus === 'TRADE_SUCCESS' || tradeStatus === 'TRADE_FINISHED') {
    status.value = 'success'
    return // already terminal, no need to poll
  }
  if (tradeStatus === 'failed' || tradeStatus === 'TRADE_CLOSED') {
    status.value = tradeStatus
    return
  }

  // No terminal status yet — poll backend every 2 s for async notify result
  if (!orderNo.value) return

  const startedAt = Date.now()
  pollTimer = setInterval(async () => {
    // Timeout — stop and leave as "确认中"
    if (Date.now() - startedAt > POLL_TIMEOUT) {
      clearInterval(pollTimer)
      pollTimer = null
      return
    }

    try {
      const data = await getOrderStatus(orderNo.value)
      if (!data) return

      // orderStatus: 1=NEW, 2=PAID, 3=CALLBACK, 4=REFUNDED, 5=EXPIRED, 6=PAY_FAILED
      const os = data.orderStatus
      if (os === 2 || os === 3) {
        // PAID / CALLBACK — terminal success
        status.value = 'success'
        amount.value = data.orderAmount != null ? String(data.orderAmount) : amount.value
        stopPolling()
      } else if (os === 5 || os === 6) {
        // EXPIRED / PAY_FAILED — terminal failure
        status.value = 'failed'
        stopPolling()
      }
      // else os === 1 (NEW) — still waiting, keep polling
    } catch {
      // network error — keep polling until timeout
    }
  }, POLL_INTERVAL)
})

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onBeforeUnmount(() => {
  stopPolling()
})

function goBack() {
  const lastMerchantNo = sessionStorage.getItem('lastMerchantNo')
  if (lastMerchantNo) {
    router.push(`/app/cashier?merchantNo=${lastMerchantNo}`)
  } else {
    router.push('/app/cashier')
  }
}
</script>

<style scoped>
.result-page {
  min-height: 100vh;
  min-height: 100dvh;
  background: #f5f6fa;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.result-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  width: 100%;
  box-sizing: border-box;
}

/* Result circle */
.result-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}
.result-circle.success {
  background: #f6ffed;
  border: 3px solid #52c41a;
}
.result-circle.fail {
  background: #fff2f0;
  border: 3px solid #ff4d4f;
}
.result-circle.pending {
  background: #fffbe6;
  border: 3px solid #faad14;
}
.result-icon {
  font-size: 36px;
  font-weight: 700;
}
.result-circle.success .result-icon { color: #52c41a; }
.result-circle.fail .result-icon { color: #ff4d4f; }
.result-circle.pending .result-icon { color: #faad14; }

.result-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.result-amount {
  font-size: 40px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 20px;
  font-family: -apple-system, BlinkMacSystemFont, sans-serif;
}

.result-desc {
  font-size: 14px;
  color: #999;
  text-align: center;
}

/* Info list */
.info-list {
  width: 100%;
  max-width: 280px;
  margin-top: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
}
.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}
.info-label {
  font-size: 14px;
  color: #999;
}
.info-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

/* Button */
.action-area {
  width: 100%;
  padding: 0 16px 32px;
  box-sizing: border-box;
}
.done-button {
  width: 100%;
  padding: 15px;
  font-size: 18px;
  font-weight: 600;
  border-radius: 12px;
  background: #1677ff;
  color: #fff;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}
.done-button:active {
  background: #0958d9;
  transform: scale(0.98);
}
</style>
