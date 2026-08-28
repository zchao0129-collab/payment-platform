import client from './client';

/** 营收统计 (今日/本周/本月) */
export function getRevenueStats() {
  return client.get('/statistics/revenue');
}
