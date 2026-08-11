<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="发起提现"
    width="520px"
    destroy-on-close
  >
    <el-form :model="form" label-position="top" v-loading="loadingData">
      <el-form-item label="可提现金额">
        <el-input :value="'¥' + withdrawable" disabled style="font-size:18px;font-weight:700;color:var(--success)" />
      </el-form-item>
      <el-form-item label="提现金额 *">
        <el-input v-model="form.amount" type="number" placeholder="请输入提现金额" :min="0.01" :max="maxAmount" />
      </el-form-item>
      <el-form-item label="提现至支付宝账号">
        <el-input :value="alipayAccount" disabled />
      </el-form-item>
      <div class="warning-tip">
        ⚠️ 提现需管理员审核，审核通过后金额将转入您的支付宝账号。
      </div>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="handleConfirm" :loading="submitting">确认提现</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getCommissionSummary, withdrawCommission } from '@/api/commission'
import { getProfile } from '@/api/merchant'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible', 'done'])

const form = reactive({ amount: '' })
const withdrawable = ref('0.00')
const alipayAccount = ref('')
const loadingData = ref(false)
const submitting = ref(false)

const maxAmount = computed(() => parseFloat(withdrawable.value) || 0)

watch(() => props.visible, async (val) => {
  if (val) {
    form.amount = ''
    loadingData.value = true
    try {
      const [summary, profile] = await Promise.all([
        getCommissionSummary(),
        getProfile().catch(() => null),
      ])
      withdrawable.value = summary?.withdrawable ? Number(summary.withdrawable).toFixed(2) : '0.00'
      alipayAccount.value = profile?.alipayAccount || '未设置'
    } catch (e) {
      console.warn('Failed to load withdraw data', e)
    } finally {
      loadingData.value = false
    }
  }
})

async function handleConfirm() {
  const amount = parseFloat(form.amount)
  if (!amount || amount <= 0) {
    ElMessage.warning('请输入有效提现金额')
    return
  }
  if (amount > parseFloat(withdrawable.value)) {
    ElMessage.warning('提现金额不能超过可提现金额')
    return
  }
  submitting.value = true
  try {
    await withdrawCommission(amount)
    ElMessage.success('提现申请已提交，请等待审核')
    emit('done')
    emit('update:visible', false)
  } catch (e) {
    ElMessage.error(e.message || '提现申请失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.warning-tip {
  font-size: 12px;
  color: #999;
  padding: 8px;
  background: #fffbe6;
  border-radius: 6px;
}
</style>
