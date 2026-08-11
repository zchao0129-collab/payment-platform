<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="新增商户"
    width="520px"
    destroy-on-close
  >
    <el-form :model="form" label-position="top">
      <el-form-item label="商户名称 *">
        <el-input v-model="form.name" placeholder="请输入商户名称" />
      </el-form-item>
      <el-form-item label="手机号 *">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="支付宝账号 *">
        <el-input v-model="form.alipayAccount" placeholder="请输入支付宝提现账号" />
      </el-form-item>
      <el-form-item label="初始密码 *">
        <el-input v-model="form.password" type="password" placeholder="设置初始登录密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确认创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'

defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible'])

const form = reactive({ name: '', phone: '', alipayAccount: '', password: '' })

function handleConfirm() {
  if (!form.name || !form.phone || !form.alipayAccount || !form.password) {
    ElMessage.warning('请填写所有必填字段')
    return
  }
  ElMessage.success('商户创建成功')
  emit('update:visible', false)
}
</script>
