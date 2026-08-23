import request from './request'

/** 微信配置列表 */
export function listWechatConfigs() {
  return request.get('/admin/wechat-config/list')
}

/** 保存微信配置 */
export function saveWechatConfig(data) {
  return request.post('/admin/wechat-config/save', data)
}

/** 删除微信配置 */
export function deleteWechatConfig(id) {
  return request.delete(`/admin/wechat-config/${id}`)
}

/** 启用微信配置 */
export function enableWechatConfig(id) {
  return request.put(`/admin/wechat-config/${id}/enable`)
}

/** 切换配置状态 */
export function toggleWechatConfigStatus(id, status) {
  return request.put(`/admin/wechat-config/${id}/status`, null, { params: { status } })
}

/** 连通性测试 */
export function testWechatConnection(id) {
  return request.post(`/admin/wechat-config/${id}/test`)
}
