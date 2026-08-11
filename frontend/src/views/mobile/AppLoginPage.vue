<template>
  <div class="phone-frame-container">
    <div class="phone-frame">
      <div class="phone-status-bar">
        <span class="time">9:41</span>
        <span class="icons">●●●●○ &nbsp;WiFi &nbsp;🔋 85%</span>
      </div>
      <div class="phone-login-page">
        <div class="phone-login-header">
          <div class="pl-logo">💰</div>
          <div class="pl-title">商户平台</div>
          <div class="pl-sub">商户端 · 登录</div>
        </div>
        <div class="phone-login-form">
          <div class="m-form-group">
            <label>手机号</label>
            <input type="text" v-model="form.phone" placeholder="请输入11位手机号" maxlength="11" />
          </div>
          <div class="m-form-group">
            <label>登录密码</label>
            <input type="password" v-model="form.password" placeholder="请输入登录密码" />
          </div>
          <div class="pl-captcha">
            <div class="pl-captcha-box">{{ captchaCode }}</div>
            <div class="pl-captcha-hint" @click="refreshCaptcha">← 请完成滑块验证</div>
          </div>
          <button class="m-btn-block m-btn-primary" @click="handleLogin">登 录</button>
          <div class="pl-links">
            <router-link to="/register">注册商户</router-link> &nbsp;|&nbsp;
            <a href="#">忘记密码？</a>
          </div>
          <div class="pl-links" style="margin-top:12px">
            <router-link to="/login" style="color:#999;font-size:12px">PC端登录 →</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const captchaCode = ref('3YpQ7')

const form = reactive({ phone: '', password: '' })

function refreshCaptcha() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  captchaCode.value = Array.from({ length: 5 }, () => chars.charAt(Math.floor(Math.random() * chars.length))).join('')
}

function handleLogin() {
  userStore.login({
    name: '张三',
    phone: '138****8888',
    role: 'merchant',
    merchantName: '张三的店铺',
    merchantNo: 'M20240001',
    referralCode: 'X8K2M9',
    alipayAccount: 'zhangsan@example.com',
  })
  router.push('/app/merchant')
}
</script>

<style scoped>
.phone-login-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f5f5;
}
.phone-login-header {
  background: linear-gradient(135deg, var(--primary), #4096ff);
  padding: 50px 24px 36px;
  text-align: center;
  color: #fff;
}
.pl-logo { font-size: 48px; margin-bottom: 8px; }
.pl-title { font-size: 22px; font-weight: 700; }
.pl-sub { font-size: 13px; opacity: 0.8; margin-top: 4px; }
.phone-login-form {
  margin: -16px 16px 0;
  background: #fff;
  border-radius: 16px;
  padding: 28px 20px 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 1;
}
.m-form-group { margin-bottom: 16px; }
.m-form-group label { display: block; margin-bottom: 6px; font-size: 13px; font-weight: 500; color: #333; }
.m-form-group input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
  transition: border-color 0.2s;
  outline: none;
}
.m-form-group input:focus { border-color: var(--primary); }
.pl-captcha {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px dashed #e8e8e8;
  margin-bottom: 16px;
}
.pl-captcha-box {
  width: 80px;
  height: 36px;
  background: linear-gradient(135deg, #e8e8e8 25%, #d8d8d8 50%, #e8e8e8 75%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  letter-spacing: 2px;
  font-style: italic;
  color: #888;
  border-radius: 4px;
  flex-shrink: 0;
  cursor: pointer;
  user-select: none;
}
.pl-captcha-hint { font-size: 11px; color: #999; cursor: pointer; }
.pl-links { text-align: center; margin-top: 16px; font-size: 13px; }
.m-btn-block {
  display: block;
  width: 100%;
  padding: 12px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 24px;
  text-align: center;
  margin-bottom: 10px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}
.m-btn-primary { background: var(--primary); color: #fff; }
.m-btn-primary:active { background: var(--primary-hover); }
</style>
