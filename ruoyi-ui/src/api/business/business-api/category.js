import request from '@/utils/request'

export function listCategory(query) { return request({ url: '/business/category/list', method: 'get', params: query }) }
export function getCategory(id) { return request({ url: '/business/category/' + id, method: 'get' }) }
export function addCategory(data) { return request({ url: '/business/category', method: 'post', data: data }) }
export function updateCategory(data) { return request({ url: '/business/category', method: 'put', data: data }) }
export function delCategory(ids) { return request({ url: '/business/category/' + ids, method: 'delete' }) }
