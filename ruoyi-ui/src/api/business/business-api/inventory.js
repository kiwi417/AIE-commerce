import request from '@/utils/request'
export function listInventory(q) { return request({ url: '/business/inventory/list', method: 'get', params: q }) }
