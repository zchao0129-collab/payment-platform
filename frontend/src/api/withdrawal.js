import request from './request'

/** 提现记录列表 */
export function getWithdrawalList(params) {
  return request.get('/withdrawal/list', { params })
}

/** [管理端] 审核通过 */
export function approveWithdrawal(withdrawalId) {
  return request.post('/withdrawal/admin/approve', { withdrawalId })
}

/** [管理端] 审核驳回 */
export function rejectWithdrawal(withdrawalId, rejectReason) {
  return request.post('/withdrawal/admin/reject', { withdrawalId, rejectReason })
}
