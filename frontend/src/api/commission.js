import request from './request'

/** 佣金列表 */
export function getCommissionList(params) {
  return request.get('/commission/list', { params })
}

/** 佣金汇总 */
export function getCommissionSummary() {
  return request.get('/commission/summary')
}

/** 发起提现 */
export function withdrawCommission(amount) {
  return request.post('/commission/withdraw', { amount })
}
