import request from './request'

/** 营收统计 */
export function getRevenueStats() {
  return request.get('/statistics/revenue')
}

/** [管理端] 订单排行 TOP10 */
export function getOrderRank() {
  return request.get('/statistics/admin/order-rank')
}

/** [管理端] 提现排行 TOP10 */
export function getWithdrawRank() {
  return request.get('/statistics/admin/withdraw-rank')
}
