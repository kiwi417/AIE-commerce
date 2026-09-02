import request from '@/utils/request'
export function listSupplier(q) { return request({ url: '/business/supplier/list', method: 'get', params: q }) }
export function getSupplier(id) { return request({ url: '/business/supplier/' + id, method: 'get' }) }
export function addSupplier(d) { return request({ url: '/business/supplier', method: 'post', data: d }) }
export function updateSupplier(d) { return request({ url: '/business/supplier', method: 'put', data: d }) }
export function delSupplier(ids) { return request({ url: '/business/supplier/' + ids, method: 'delete' }) }
