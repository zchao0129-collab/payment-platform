import client from './client';

/** 获取我的码牌 */
export function getMyQrcode() {
  return client.get('/qrcode/my');
}

/** 重新生成码牌 */
export function regenerateQrcode() {
  return client.post('/qrcode/regenerate');
}
