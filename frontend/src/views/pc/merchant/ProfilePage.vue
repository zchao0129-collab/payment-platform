<template>
  <div>
    <div class="page-title">商户信息</div>
    <div class="card">
      <div class="card-header">基础资料</div>
      <el-form :model="form" label-width="140px" class="config-form" v-loading="loading">
        <el-form-item label="商户名称">
          <el-input v-model="form.merchantName" />
        </el-form-item>
        <el-form-item label="商户号">
          <el-input v-model="form.merchantNo" disabled />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="支付宝提现账号">
          <el-input v-model="form.alipayAccount" />
        </el-form-item>
        <el-form-item label="推荐码">
          <el-input v-model="form.referralCode" disabled />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存修改</el-button>
          <el-button @click="passwordVisible = true">修改密码</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Password Modal -->
    <PasswordModal v-model:visible="passwordVisible" />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile } from '@/api/merchant'
import PasswordModal from '@/components/PasswordModal.vue'

const passwordVisible = ref(false)
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  merchantName: '',
  merchantNo: '',
  phone: '',
  alipayAccount: '',
  referralCode: '',
})

onMounted(async () => {
  loading.value = true
  try {
    const data = await getProfile()
    if (data) {
      form.merchantName = data.merchantName || ''
      form.merchantNo = data.merchantNo || ''
      form.phone = data.phone || ''
      form.alipayAccount = data.alipayAccount || ''
      form.referralCode = data.referralCode || ''
    }
  } catch (e) {
    ElMessage.error(e.message || '加载商户信息失败')
  } finally {
    loading.value = false
  }
})

async function handleSave() {
  saving.value = true
  try {
    await updateProfile({
      merchantName: form.merchantName,
      alipayAccount: form.alipayAccount,
    })
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.config-form {
  max-width: 600px;
}
</style>
