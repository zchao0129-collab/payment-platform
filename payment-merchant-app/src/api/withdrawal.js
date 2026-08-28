import client from './client';

/** 提现记录列表 */
export function getWithdrawalList(params) {
  return client.get('/withdrawal/list', { params });
}
