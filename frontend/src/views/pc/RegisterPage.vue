<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>商户入驻</h2>
      <div class="subtitle">填写信息注册成为平台商户</div>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="手机号 *" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入11位手机号" maxlength="11" />
        </el-form-item>

        <el-form-item label="机器人校验 *">
          <div class="captcha-area">
            <div class="captcha-box">{{ captchaCode }}</div>
            <span class="captcha-hint" @click="refreshCaptcha">
              ← 拖动滑块完成拼图验证 &nbsp;<a href="#">换一张</a>
            </span>
          </div>
        </el-form-item>

        <el-form-item label="短信验证码 *" prop="smsCode">
          <div class="sms-row">
            <el-input v-model="form.smsCode" placeholder="6位验证码" maxlength="6" style="flex:1" />
            <el-button type="primary" :disabled="smsCountdown > 0" @click="sendSms" class="sms-btn">
              {{ smsCountdown > 0 ? smsCountdown + 's后重发' : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="支付宝提现账号 *" prop="alipayAccount">
          <el-input v-model="form.alipayAccount" placeholder="请输入支付宝账号" />
        </el-form-item>

        <el-form-item label="登录密码 *" prop="password">
          <el-input v-model="form.password" type="password" placeholder="8-20位，含字母+数字" />
        </el-form-item>

        <el-form-item label="确认密码 *" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" />
        </el-form-item>

        <el-form-item label="推荐码">
          <el-input v-model="form.referralCode" placeholder="选填，输入邀请人的推荐码" />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="form.agreed">
            我已阅读并同意《平台服务协议》
          </el-checkbox>
        </el-form-item>

        <el-button type="primary" size="large" style="width:100%;font-weight:600" @click="handleRegister">
          注 册
        </el-button>
      </el-form>

      <div class="form-footer">
        已有账号？<router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as authApi from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const smsCountdown = ref(0)
const captchaCode = ref('W8Km2')

const form = reactive({
  phone: '',
  smsCode: '',
  alipayAccount: '',
  password: '',
  confirmPassword: '',
  referralCode: '',
  agreed: false,
})

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  smsCode: [
    { required: true, message: '请输入短信验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入6位验证码', trigger: 'blur' },
  ],
  alipayAccount: [
    { required: true, message: '请输入支付宝提现账号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d).{8,20}$/, message: '8-20位，含字母+数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次密码输入不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
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

async function sendSms() {
  if (smsCountdown.value > 0) return
  try {
    // Get captcha ticket first (scene=1 for registration)
    const ticket = await authApi.getCaptchaTicket(1)
    await authApi.sendSms(form.phone, 1, ticket)
    smsCountdown.value = 60
    const timer = setInterval(() => {
      smsCountdown.value--
      if (smsCountdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
    ElMessage.success('验证码已发送')
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
  }
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!form.agreed) {
    ElMessage.warning('请阅读并同意平台服务协议')
    return
  }
  try {
    const ticket = await authApi.getCaptchaTicket(1)
    await authApi.register({
      phone: form.phone,
      smsCode: form.smsCode,
      alipayAccount: form.alipayAccount,
      password: form.password,
      confirmPassword: form.confirmPassword,
      captchaTicket: ticket,
      referralCode: form.referralCode || undefined,
    })
    ElMessage.success('注册成功，即将跳转登录页')
    setTimeout(() => router.push('/login'), 1500)
  } catch (e) {
    ElMessage.error(e.message || '注册失败')
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
  background: linear-gradient(135deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
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
.sms-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.sms-row .sms-btn {
  min-width: 120px;
  white-space: nowrap;
}
.form-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
}
</style>
