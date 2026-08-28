import client from './client';

/** 获取商户信息 */
export function getProfile() {
  return client.get('/merchant/profile');
}

/** 更新商户信息 */
export function updateProfile(data) {
  return client.put('/merchant/profile', data);
}

/** 修改密码 */
export function changePassword(data) {
  return client.put('/merchant/password', data);
}
