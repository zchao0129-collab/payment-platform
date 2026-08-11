import request from './request'

/** 获取/生成码牌 */
export function getMyQrcode() {
  return request.get('/qrcode/my')
}

/** 重新生成码牌 */
export function regenerateQrcode() {
  return request.post('/qrcode/regenerate')
}

/** 收银台获取码牌商户信息（无需登录） */
export function getQrcodeInfo(merchantNo) {
  return request.get('/qrcode/info', { params: { merchantNo } })
}
