<template>
  <div class="cashier-body">
    <div class="cashier-card">
      <!-- Merchant header -->
      <div class="merchant-bar">
        <div class="m-name">🏪 {{ merchant.merchantName || '加载中...' }}</div>
        <div style="font-size:11px;opacity:.85;margin-top:4px">商户号：{{ merchant.merchantNo || '—' }}</div>
      </div>

      <div class="cashier-body-inner">
        <!-- Input state -->
        <template v-if="cashierState === 'input'">
          <div class="amount-section">
            <label>请输入付款金额</label>
            <div class="amount-input">
              <span class="currency">¥</span>
              <input type="number" v-model="amount" placeholder="0.00" step="0.01" min="0.01" />
            </div>
            <div style="margin-top:12px">
              <input type="text" v-model="note" placeholder="备注（选填）"
                     style="border:1px solid #e8e8e8;border-radius:8px;padding:8px 12px;width:100%;font-size:13px;outline:none" />
            </div>
          </div>
          <button class="pay-btn" @click="submitPayment" :disabled="submitting">
            {{ submitting ? '处理中...' : '确认支付' }}
          </button>
          <div style="text-align:center;margin-top:12px;font-size:12px;color:#bbb">
            支付服务由支付宝提供
          </div>
        </template>

        <!-- Loading state -->
        <template v-if="cashierState === 'loading'">
          <div class="state-center">
            <div class="spinner"></div>
            <div style="color:#999;margin-top:12px">正在创建订单...</div>
            <div style="color:#bbb;font-size:12px;margin-top:4px">请稍候，不要关闭页面</div>
          </div>
        </template>

        <!-- Success state -->
        <template v-if="cashierState === 'success'">
          <div class="state-center">
            <div class="result-icon success">✓</div>
            <div style="font-size:20px;font-weight:700;margin-top:12px">订单已创建</div>
            <div style="font-size:28px;font-weight:700;margin:12px 0">¥{{ paidAmount }}</div>
            <div style="font-size:13px;color:#999">订单号：{{ orderNo }}</div>
            <div style="font-size:13px;color:#999">商户：{{ merchant.merchantName }}</div>
            <div style="font-size:13px;color:#1677ff;margin-top:8px">请完成支付宝支付</div>
            <button class="pay-btn" @click="reset" style="margin-top:20px">完 成</button>
          </div>
        </template>

        <!-- Error state -->
        <template v-if="cashierState === 'error'">
          <div class="state-center">
            <div class="result-icon fail">✕</div>
            <div style="font-size:20px;font-weight:700;margin-top:12px">创建订单失败</div>
            <div style="font-size:13px;color:#999;margin-top:8px">{{ errorMsg }}</div>
            <button class="pay-btn" @click="submitPayment" style="margin-top:20px">重 试</button>
            <div style="text-align:center;margin-top:10px">
              <a href="#" @click.prevent="cashierState='input'" style="color:#999;font-size:13px">返回修改金额</a>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
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
}
</script>

<style scoped>
.cashier-body {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
.cashier-card {
  width: 380px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}
.merchant-bar {
  padding: 16px 20px;
  background: var(--primary);
  color: #fff;
  text-align: center;
}
.m-name {
  font-size: 16px;
  font-weight: 600;
}
.cashier-body-inner {
  padding: 24px 20px;
}
.amount-section {
  margin-bottom: 20px;
}
.amount-section label {
  display: block;
  margin-bottom: 8px;
  color: #999;
  font-size: 13px;
}
.amount-input {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  border-bottom: 2px solid var(--primary);
  padding-bottom: 8px;
}
.currency {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}
.amount-input input {
  border: none;
  font-size: 36px;
  font-weight: 700;
  text-align: center;
  width: 180px;
  padding: 0;
  outline: none;
}
.pay-btn {
  width: 100%;
  padding: 14px;
  font-size: 18px;
  font-weight: 600;
  border-radius: 6px;
  background: var(--primary);
  color: #fff;
  border: none;
  cursor: pointer;
}
.pay-btn:hover { background: var(--primary-hover); }
.pay-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.state-center { text-align: center; padding: 20px 0; }
.spinner {
  width: 48px; height: 48px;
  border: 4px solid #e8e8e8;
  border-top-color: var(--primary);
  border-radius: 50%;
  margin: 0 auto;
  animation: spin .8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.result-icon {
  width: 72px; height: 72px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
}
.result-icon.success { background: #f6ffed; color: var(--success); border: 3px solid var(--success); }
.result-icon.fail { background: #fff2f0; color: var(--danger); border: 3px solid var(--danger); }
</style>
