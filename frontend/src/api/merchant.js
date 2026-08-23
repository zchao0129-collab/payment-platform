import request from './request'

/** 获取当前商户信息 */
export function getProfile() {
  return request.get('/merchant/profile')
}

/** 修改商户信息 */
export function updateProfile(data) {
  return request.put('/merchant/profile', data)
}

/** 修改密码 */
export function changePassword(oldPassword, newPassword) {
  return request.put('/merchant/password', { oldPassword, newPassword })
}

/** [管理端] 商户列表 */
export function getMerchantList(params) {
  return request.get('/merchant/admin/list', { params })
}

/** [管理端] 新增商户 */
export function createMerchant(data) {
  return request.post('/merchant/admin/create', data)
}

/** [管理端] 修改商户信息 */
export function updateMerchant(id, data) {
  return request.put(`/merchant/admin/${id}`, data)
}

/** [管理端] 停用/启用商户 */
export function toggleMerchantStatus(id, status) {
  return request.put(`/merchant/admin/${id}/status`, null, { params: { status } })
}

/** [管理端] 重置商户API密钥 */
export function resetApiSecret(id) {
  return request.post(`/merchant/admin/${id}/api-secret`)
}

/** [管理端] 获取商户码牌 */
export function getMerchantQrcode(merchantId) {
  return request.get(`/merchant/admin/${merchantId}/qrcode`)
}

/** [管理端] 配置商户开放API（开关/回调地址/IP白名单） */
export function updateApiConfig(id, data) {
  return request.put(`/merchant/admin/${id}/api-config`, data)
}

/** [管理端] 删除商户 */
export function deleteMerchant(id) {
  return request.delete(`/merchant/admin/${id}`)
}
