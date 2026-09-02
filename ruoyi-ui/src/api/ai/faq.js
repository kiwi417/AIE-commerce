import request from '@/utils/request'

export function listFaq(query) {
  return request({ url: '/business/faq/list', method: 'get', params: query })
}

export function getFaq(id) {
  return request({ url: '/business/faq/' + id, method: 'get' })
}

export function addFaq(data) {
  return request({ url: '/business/faq', method: 'post', data })
}

export function updateFaq(data) {
  return request({ url: '/business/faq', method: 'put', data })
}

export function delFaq(id) {
  return request({ url: '/business/faq/' + id, method: 'delete' })
}

export function importTemplate() {
  return request({ url: '/business/faq/importTemplate', method: 'get' })
}
