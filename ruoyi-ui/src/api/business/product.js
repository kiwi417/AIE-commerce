import request from '@/utils/request'

export function listProduct(query) { return request({ url: '/business/product/list', method: 'get', params: query }) }
export function getProduct(id) { return request({ url: '/business/product/' + id, method: 'get' }) }
export function addProduct(data) { return request({ url: '/business/product', method: 'post', data: data }) }
export function updateProduct(data) { return request({ url: '/business/product', method: 'put', data: data }) }
export function delProduct(ids) { return request({ url: '/business/product/' + ids, method: 'delete' }) }
export function exportProduct(query) { return request({ url: '/business/product/export', method: 'post', params: query }) }
export function importTemplate() { return request({ url: '/business/product/importTemplate', method: 'get' }) }
