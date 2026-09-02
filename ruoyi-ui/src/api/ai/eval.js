import request from '@/utils/request'

export function listEvalLog(query) {
  return request({ url: '/ai/eval/list', method: 'get', params: query })
}

export function getEvalLog(id) {
  return request({ url: '/ai/eval/' + id, method: 'get' })
}

export function delEvalLog(ids) {
  return request({ url: '/ai/eval/' + ids, method: 'delete' })
}

export function getEvalStats() {
  return request({ url: '/ai/eval/stats', method: 'get' })
}
