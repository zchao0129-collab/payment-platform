import client from './client';

/** 订单列表 */
export function getOrderList(params) {
  return client.get('/order/list', { params });
}

/** 订单详情 */
export function getOrderDetail(id) {
  return client.get(`/order/${id}`);
}
