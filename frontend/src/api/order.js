import request from './request'

/** 收银台创建订单 (公开) */
export function createOrder(data) {
  return request.post('/order/create', data)
}

/** 查询订单状态 (公开 — 支付结果页轮询) */
export function getOrderStatus(orderNo) {
  return request.get('/order/status', { params: { orderNo } })
}

/** 订单列表 */
export function getOrderList(params) {
  return request.get('/order/list', { params })
}

/** 订单详情 */
export function getOrderDetail(id) {
  return request.get(`/order/${id}`)
}

/** 全额退款 */
export function refundOrder(id) {
  return request.post(`/order/${id}/refund`)
}
