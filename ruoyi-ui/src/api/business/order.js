import request from '@/utils/request'
export function listOrder(q) { return request({ url: '/business/order/list', method: 'get', params: q }) }
export function getOrder(id) { return request({ url: '/business/order/' + id, method: 'get' }) }
export function completeOrder(id) { return request({ url: '/business/order/' + id + '/complete', method: 'put' }) }
export function cancelOrder(id) { return request({ url: '/business/order/' + id + '/cancel', method: 'put' }) }
