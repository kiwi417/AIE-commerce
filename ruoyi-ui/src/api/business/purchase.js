import request from '@/utils/request'
export function listPurchase(q) { return request({ url: '/business/purchase/list', method: 'get', params: q }) }
export function getPurchase(id) { return request({ url: '/business/purchase/' + id, method: 'get' }) }
export function addPurchase(d) { return request({ url: '/business/purchase', method: 'post', data: d }) }
export function updatePurchase(d) { return request({ url: '/business/purchase', method: 'put', data: d }) }
export function delPurchase(ids) { return request({ url: '/business/purchase/' + ids, method: 'delete' }) }
export function confirmPurchase(id) { return request({ url: '/business/purchase/' + id + '/confirm', method: 'put' }) }
export function receivePurchase(id) { return request({ url: '/business/purchase/' + id + '/receive', method: 'put' }) }
export function cancelPurchase(id) { return request({ url: '/business/purchase/' + id + '/cancel', method: 'put' }) }
