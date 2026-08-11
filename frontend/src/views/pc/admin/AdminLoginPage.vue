<template>
  <div class="admin-auth-page">
    <div class="auth-card">
      <div class="admin-badge">🔐 管理后台</div>
      <h2>管理员登录</h2>
      <div class="subtitle">支付商户管理平台 · 系统管理</div>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入管理员手机号" />
        </el-form-item>

        <el-form-item label="登录密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" @keyup.enter="handleLogin" />
        </el-form-item>

        <el-form-item label="安全校验">
          <div class="captcha-area">
            <div class="captcha-box" @click="refreshCaptcha">{{ captchaCode }}</div>
            <span class="captcha-hint">
              ← 点击验证码刷新 &nbsp;<a href="#" @click.prevent="refreshCaptcha">换一张</a>
            </span>
          </div>
        </el-form-item>

        <el-button type="primary" size="large" style="width:100%;font-weight:600;margin-top:8px" @click="handleLogin" :loading="loading">
          管理员登录
        </el-button>
      </el-form>

      <div class="form-footer">
        <router-link to="/login">← 商户登录</router-link>
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
const captchaCode = ref('A7kP2')
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
    const ticket = await authApi.getCaptchaTicket(2)
    const resp = await userStore.loginAction({
      phone: form.phone,
      password: form.password,
      captchaTicket: ticket,
    })

    // Only admin (role === 1) can enter
    if (resp.role !== 1) {
      userStore.clearSession()
      ElMessage.error('该账号非管理员，无法登录管理后台')
      loading.value = false
      return
    }

    ElMessage.success('管理员登录成功')
    router.push('/admin/dashboard')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #141e30, #243b55);
}

.admin-auth-page .auth-card {
  width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.admin-auth-page .auth-card h2 {
  text-align: center;
  margin-bottom: 8px;
  font-size: 24px;
  color: #1a1a2e;
}

.admin-auth-page .auth-card .subtitle {
  text-align: center;
  color: #999;
  margin-bottom: 28px;
  font-size: 13px;
}

.admin-badge {
  text-align: center;
  margin-bottom: 12px;
}

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
  background: linear-gradient(135deg, #243b55, #141e30);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  letter-spacing: 3px;
  font-style: italic;
  color: #8ab4f8;
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
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e8e8e8;
  font-size: 13px;
}
</style>
