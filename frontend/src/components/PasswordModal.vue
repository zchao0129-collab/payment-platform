<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="修改密码"
    width="520px"
    destroy-on-close
  >
    <el-form :model="form" label-position="top">
      <el-form-item label="原密码 *">
        <el-input v-model="form.oldPassword" type="password" placeholder="请输入原密码" />
      </el-form-item>
      <el-form-item label="新密码 *">
        <el-input v-model="form.newPassword" type="password" placeholder="8-20位，含字母+数字" />
      </el-form-item>
      <el-form-item label="确认新密码 *">
        <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入新密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="handleConfirm" :loading="submitting">确认修改</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/api/merchant'

defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible'])

const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const submitting = ref(false)

async function handleConfirm() {
  if (!form.oldPassword || !form.newPassword || !form.confirmPassword) {
    ElMessage.warning('请填写所有字段')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  if (!/^(?=.*[a-zA-Z])(?=.*\d).{8,20}$/.test(form.newPassword)) {
    ElMessage.warning('密码需8-20位，含字母+数字')
    return
  }
  submitting.value = true
  try {
    await changePassword(form.oldPassword, form.newPassword)
    ElMessage.success('密码修改成功')
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    emit('update:visible', false)
  } catch (e) {
    ElMessage.error(e.message || '密码修改失败')
  } finally {
    submitting.value = false
  }
}
</script>
