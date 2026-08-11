import request from './request'

/** 发送短信验证码 */
export function sendSms(phone, scene, captchaTicket) {
  return request.post('/auth/sms/send', { phone, scene, captchaTicket })
}

/** 获取 captcha ticket */
export function getCaptchaTicket(scene) {
  return request.get('/auth/captcha/ticket', { params: { scene } })
}

/** 商户注册 */
export function register(data) {
  return request.post('/auth/register', data)
}

/** 登录 */
export function login(data) {
  return request.post('/auth/login', data)
}

/** 刷新 token */
export function refreshToken(refreshToken) {
  return request.post('/auth/refresh', null, { params: { refreshToken } })
}

/** 登出 */
export function logout() {
  return request.post('/auth/logout')
}
