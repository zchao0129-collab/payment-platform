import request from './request'

// ========== 支付宝配置 ==========
export function getAlipayConfigList() {
  return request.get('/admin/alipay-config/list')
}

/** 上传证书文件，返回服务器存储路径 */
export function uploadCertFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/alipay-config/upload-cert', formData)
}

export function saveAlipayConfig(data) {
  return request.post('/admin/alipay-config/save', data)
}

export function deleteAlipayConfig(id) {
  return request.delete(`/admin/alipay-config/${id}`)
}

export function enableAlipayConfig(id) {
  return request.put(`/admin/alipay-config/${id}/enable`)
}

export function toggleAlipayConfigStatus(id, status) {
  return request.put(`/admin/alipay-config/${id}/status`, null, { params: { status } })
}

export function testAlipayConnection(id) {
  return request.post(`/admin/alipay-config/${id}/test`)
}

// ========== 返佣配置 ==========
export function getCommConfigList() {
  return request.get('/admin/comm-config/list')
}

export function addCommConfig(data) {
  return request.post('/admin/comm-config/add', data)
}

export function updateCommConfig(id, data) {
  return request.put(`/admin/comm-config/${id}`, data)
}

export function deleteCommConfig(id) {
  return request.delete(`/admin/comm-config/${id}`)
}

// ========== 用户管理 ==========
export function getUserList(params) {
  return request.get('/admin/user/list', { params })
}

export function createUser(data) {
  return request.post('/admin/user/create', data)
}

export function updateUser(id, data) {
  return request.put(`/admin/user/${id}`, data)
}

export function toggleUserStatus(id, status) {
  return request.put(`/admin/user/${id}/status`, null, { params: { status } })
}
