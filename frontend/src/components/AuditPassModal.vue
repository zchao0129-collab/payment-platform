<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="审核通过确认"
    width="520px"
    destroy-on-close
  >
    <p style="margin-bottom:12px">确认通过 张三的店铺 的提现申请？</p>
    <div class="info-box">
      提现金额：<strong>¥2,000.00</strong><br />
      支付宝账号：zhang***@example.com
    </div>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="success" @click="handleConfirm">确认通过</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ElMessage } from 'element-plus'

defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible'])

function handleConfirm() {
  ElMessage.success('审核通过，已自动打款')
  emit('update:visible', false)
}
</script>

<style scoped>
.info-box {
  padding: 12px;
  background: #f6ffed;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.8;
}
</style>
