import client from './client';

/** 获取验证票据（登录前必须调用） */
export function getCaptchaTicket(scene = 1) {
  return client.get('/auth/captcha/ticket', { params: { scene } });
}

/** 商户登录 */
export function login(data) {
  return client.post('/auth/login', data);
}

/** 刷新令牌 */
export function refreshToken(refreshToken) {
  return client.post('/auth/refresh', null, { params: { refreshToken } });
}
