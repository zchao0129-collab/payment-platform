<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>商户登录</h2>
      <div class="subtitle">支付商户管理平台</div>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item label="登录密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" @keyup.enter="handleLogin" />
        </el-form-item>

        <el-form-item label="机器人校验">
          <div class="captcha-area">
            <div class="captcha-box">{{ captchaCode }}</div>
            <span class="captcha-hint" @click="refreshCaptcha">
              ← 请完成滑块验证 &nbsp;<a href="#">换一张</a>
            </span>
          </div>
        </el-form-item>

        <el-button type="primary" size="large" style="width:100%;font-weight:600;margin-top:8px" @click="handleLogin" :loading="loading">
          登 录
        </el-button>
      </el-form>

      <div class="form-footer">
        <router-link to="/register">注册商户</router-link> &nbsp;|&nbsp;
        <a href="#">忘记密码？</a>
      </div>
      <div class="admin-link">
        <router-link to="/admin/login" style="color:#999;font-size:12px">
          管理员登录 →
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import * as authApi from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const captchaCode = ref('3YpQ7')
const loading = ref(false)

const form = reactive({
  phone: '',
  password: '',
})

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
  ],
}

function refreshCaptcha() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let code = ''
  for (let i = 0; i < 5; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  captchaCode.value = code
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    // Get captcha ticket first
    const ticket = await authApi.getCaptchaTicket(2) // scene=2 for login

    const resp = await userStore.loginAction({
      phone: form.phone,
      password: form.password,
      captchaTicket: ticket,
    })

    // Only merchant (role === 2) can enter via this page
    if (resp.role === 1) {
      userStore.clearSession()
      ElMessage.error('管理员请使用管理端登录')
      loading.value = false
      return
    }

    ElMessage.success('登录成功')
    router.push('/merchant/dashboard')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.captcha-area {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
  border: 1px dashed #e8e8e8;
}
.captcha-box {
  width: 100px;
  height: 40px;
  background: linear-gradient(135deg, #e8e8e8 25%, #d0d0d0 50%, #e8e8e8 75%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  letter-spacing: 3px;
  font-style: italic;
  color: #888;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
}
.captcha-hint {
  font-size: 12px;
  color: #999;
}
.form-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
}
.admin-link {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e8e8e8;
  text-align: center;
}
</style>
