<template>
  <div class="cashier-page">
    <!-- State 1: Input Amount -->
    <template v-if="cashierState === 'input'">
      <div class="merchant-header">
        <div class="merchant-icon">🏪</div>
        <div class="merchant-name">{{ merchant.merchantName || '加载中...' }}</div>
        <div class="merchant-no">商户号：{{ merchant.merchantNo || '—' }}</div>
      </div>

      <div class="amount-card">
        <div class="amount-label">请输入付款金额</div>
        <div class="amount-input-row">
          <span class="currency-symbol">¥</span>
          <input
            ref="amountInput"
            v-model="amount"
            type="number"
            placeholder="0.00"
            step="0.01"
            min="0.01"
            class="amount-field"
            autofocus
          />
        </div>
        <div class="remark-row">
          <input
            v-model="note"
            type="text"
            placeholder="添加备注（选填）"
            class="remark-field"
          />
        </div>
      </div>

      <div class="action-area">
        <button
          class="pay-button"
          :class="{ loading: submitting }"
          :disabled="submitting"
          @click="submitPayment"
        >
          {{ submitting ? '处理中...' : '确认支付' }}
        </button>
        <div class="footer-hint">
          <span class="shield-icon">🛡️</span>
          支付服务由支付宝提供
        </div>
      </div>
    </template>

    <!-- State 2: Loading -->
    <template v-if="cashierState === 'loading'">
      <div class="state-center">
        <div class="spinner"></div>
        <div class="state-title">正在创建订单...</div>
        <div class="state-desc">请稍候，不要关闭页面</div>
      </div>
    </template>

    <!-- State 3: Success -->
    <template v-if="cashierState === 'success'">
      <div class="state-center">
        <div class="result-circle success">
          <span class="result-icon">✓</span>
        </div>
        <div class="state-title">订单已创建</div>
        <div class="result-amount">¥{{ paidAmount }}</div>
        <div class="info-list">
          <div class="info-item">
            <span class="info-label">订单号</span>
            <span class="info-value">{{ orderNo }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">商户</span>
            <span class="info-value">{{ merchant.merchantName }}</span>
          </div>
        </div>
        <div class="pay-hint">请在支付宝中完成支付</div>
        <button class="pay-button" @click="reset" style="margin-top:24px">完 成</button>
      </div>
    </template>

    <!-- State 4: Error -->
    <template v-if="cashierState === 'error'">
      <div class="state-center">
        <div class="result-circle fail">
          <span class="result-icon">✕</span>
        </div>
        <div class="state-title">订单创建失败</div>
        <div class="error-msg">{{ errorMsg }}</div>
        <button class="pay-button" style="margin-top:24px" @click="submitPayment">重 试</button>
        <a class="back-link" @click.prevent="cashierState='input'">返回修改金额</a>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import * as qrcodeApi from '@/api/qrcode'
import * as orderApi from '@/api/order'

const route = useRoute()

const cashierState = ref('input')
const amount = ref('')
const note = ref('')
const submitting = ref(false)
const paidAmount = ref('')
const orderNo = ref('')
const errorMsg = ref('')
const amountInput = ref(null)

const merchant = reactive({
  merchantId: null,
  merchantNo: '',
  merchantName: '',
  alipayConfigId: null,
})

onMounted(async () => {
  const merchantNo = route.query.merchantNo
  if (!merchantNo) {
    cashierState.value = 'error'
    errorMsg.value = '缺少商户号参数，无效的二维码'
    return
  }
  merchant.merchantNo = merchantNo

  try {
    const info = await qrcodeApi.getQrcodeInfo(merchantNo)
    if (info) {
      merchant.merchantId = info.merchantId
      merchant.merchantName = info.merchantName
      merchant.alipayConfigId = info.alipayConfigId
    }
  } catch (e) {
    cashierState.value = 'error'
    errorMsg.value = e.message || '获取商户信息失败'
  }

  // Auto-focus amount input
  await nextTick()
  amountInput.value?.focus()
})

async function submitPayment() {
  const val = parseFloat(amount.value)
  if (!val || val <= 0) {
    errorMsg.value = '请输入有效金额'
    cashierState.value = 'error'
    return
  }
  if (!merchant.merchantId) {
    errorMsg.value = '商户信息未加载，请刷新重试'
    cashierState.value = 'error'
    return
  }

  cashierState.value = 'loading'
  submitting.value = true

  try {
    const result = await orderApi.createOrder({
      merchantId: merchant.merchantId,
      amount: val.toFixed(2),
      productName: note.value || '扫码支付',
      remark: note.value || '',
    })
    paidAmount.value = val.toFixed(2)
    orderNo.value = result?.orderNo || ''

    // 如果有支付宝支付表单，渲染表单自动跳转至支付宝完成支付
    if (result?.alipayForm) {
      // 存储商户号，支付完成后可通过"完成"按钮返回该商户的收银台
      sessionStorage.setItem('lastMerchantNo', merchant.merchantNo)
      document.write(result.alipayForm)
      document.close()
      return
    }

    // 无支付宝表单（例如未配置支付宝），展示成功页作为兜底
    cashierState.value = 'success'
  } catch (e) {
    errorMsg.value = e.message || '创建订单失败，请重试'
    cashierState.value = 'error'
  } finally {
    submitting.value = false
  }
}

function reset() {
  cashierState.value = 'input'
  amount.value = ''
  note.value = ''
  errorMsg.value = ''
  nextTick(() => amountInput.value?.focus())
}
</script>

<style scoped>
/* ----- 基础布局：全屏 H5，无手机框 ----- */
.cashier-page {
  min-height: 100vh;
  min-height: 100dvh; /* 动态 viewport，适配 Safari 底部栏 */
  background: #f5f6fa;
  display: flex;
  flex-direction: column;
}

/* ====== 商户头部 ====== */
.merchant-header {
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
  padding: 32px 20px 28px;
  text-align: center;
  color: #fff;
}
.merchant-icon {
  font-size: 44px;
  margin-bottom: 8px;
}
.merchant-name {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.merchant-no {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 6px;
  font-family: monospace;
}

/* ====== 金额输入卡片 ====== */
.amount-card {
  margin: -12px 16px 0;
  background: #fff;
  border-radius: 16px;
  padding: 24px 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  position: relative;
  z-index: 1;
}
.amount-label {
  font-size: 14px;
  color: #999;
  text-align: center;
  margin-bottom: 16px;
}
.amount-input-row {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  padding-bottom: 12px;
  border-bottom: 2px solid #1677ff;
}
.currency-symbol {
  font-size: 32px;
  font-weight: 600;
  color: #333;
}
.amount-field {
  border: none;
  font-size: 48px;
  font-weight: 700;
  text-align: center;
  width: 200px;
  padding: 0;
  outline: none;
  background: transparent;
  color: #1a1a1a;
  caret-color: #1677ff;
}
.amount-field::-webkit-inner-spin-button,
.amount-field::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.amount-field::placeholder {
  color: #d0d0d0;
  font-size: 36px;
}

.remark-row {
  margin-top: 14px;
}
.remark-field {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #eee;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  background: #fafafa;
  color: #333;
  box-sizing: border-box;
  transition: border-color 0.2s;
}
.remark-field:focus {
  border-color: #1677ff;
  background: #fff;
}
.remark-field::placeholder {
  color: #ccc;
}

/* ====== 按钮区域 ====== */
.action-area {
  padding: 0 16px;
  margin-top: 24px;
}
.pay-button {
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
  letter-spacing: 1px;
}
.pay-button:active {
  background: #0958d9;
  transform: scale(0.98);
}
.pay-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.footer-hint {
  text-align: center;
  font-size: 12px;
  color: #bbb;
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.shield-icon {
  font-size: 13px;
}

/* ====== 状态居中页面 ====== */
.state-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background: #fff;
  min-height: 70vh;
}
.state-title {
  font-size: 20px;
  font-weight: 700;
  margin-top: 20px;
  color: #1a1a1a;
}
.state-desc {
  font-size: 14px;
  color: #999;
  margin-top: 8px;
}

/* Spinner */
.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #e8e8e8;
  border-top-color: #1677ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 结果图标 */
.result-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.result-circle.success {
  background: #f6ffed;
  border: 3px solid #52c41a;
}
.result-circle.fail {
  background: #fff2f0;
  border: 3px solid #ff4d4f;
}
.result-icon {
  font-size: 36px;
  font-weight: 700;
}
.result-circle.success .result-icon {
  color: #52c41a;
}
.result-circle.fail .result-icon {
  color: #ff4d4f;
}

.result-amount {
  font-size: 36px;
  font-weight: 700;
  color: #1a1a1a;
  margin-top: 12px;
  font-family: -apple-system, BlinkMacSystemFont, sans-serif;
}

/* 订单信息 */
.info-list {
  width: 100%;
  max-width: 280px;
  margin-top: 16px;
  background: #fafafa;
  border-radius: 12px;
  padding: 12px 16px;
}
.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}
.info-item + .info-item {
  border-top: 1px solid #f0f0f0;
}
.info-label {
  font-size: 13px;
  color: #999;
}
.info-value {
  font-size: 13px;
  color: #333;
  font-weight: 500;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pay-hint {
  font-size: 14px;
  color: #1677ff;
  margin-top: 16px;
  font-weight: 500;
}

.error-msg {
  font-size: 14px;
  color: #999;
  margin-top: 8px;
  text-align: center;
  padding: 0 20px;
}

.back-link {
  display: inline-block;
  margin-top: 16px;
  color: #999;
  font-size: 13px;
  text-decoration: none;
}
.back-link:active {
  color: #1677ff;
}
</style>
