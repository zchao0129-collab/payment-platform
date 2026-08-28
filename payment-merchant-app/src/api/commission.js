import client from './client';

/** 佣金列表 */
export function getCommissionList(params) {
  return client.get('/commission/list', { params });
}

/** 佣金汇总 */
export function getCommissionSummary() {
  return client.get('/commission/summary');
}

/** 发起提现 */
export function withdrawCommission(amount) {
  return client.post('/commission/withdraw', { amount });
}
