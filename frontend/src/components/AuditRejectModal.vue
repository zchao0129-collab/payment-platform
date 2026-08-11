<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="驳回提现申请"
    width="520px"
    destroy-on-close
  >
    <el-form :model="form" label-position="top">
      <el-form-item>
        <span>提现单号：{{ withdrawal?.withdrawalNo || '—' }}</span>
        &nbsp;&nbsp;金额：¥{{ fmt(withdrawal?.amount) }}
      </el-form-item>
      <el-form-item label="驳回原因 *">
        <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请填写驳回原因" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="danger" @click="handleConfirm" :loading="submitting">确认驳回</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { rejectWithdrawal } from '@/api/withdrawal'

const props = defineProps({ visible: Boolean, withdrawal: Object })
const emit = defineEmits(['update:visible', 'done'])

const form = reactive({ reason: '' })
const submitting = ref(false)

function fmt(v) { return v != null ? Number(v).toFixed(2) : '0.00' }

async function handleConfirm() {
  if (!form.reason.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  if (!props.withdrawal?.id) {
    ElMessage.error('缺少提现单信息')
    return
  }
  submitting.value = true
  try {
    await rejectWithdrawal(props.withdrawal.id, form.reason.trim())
    ElMessage.success('已驳回提现申请')
    form.reason = ''
    emit('done')
    emit('update:visible', false)
  } catch (e) {
    ElMessage.error(e.message || '驳回操作失败')
  } finally {
    submitting.value = false
  }
}
</script>
