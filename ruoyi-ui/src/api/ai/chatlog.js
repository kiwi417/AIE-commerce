import request from '@/utils/request'

export function listChatLog(query) {
  return request({ url: '/ai/chatlog/list', method: 'get', params: query })
}

export function getChatLog(id) {
  return request({ url: '/ai/chatlog/' + id, method: 'get' })
}

export function delChatLog(ids) {
  return request({ url: '/ai/chatlog/' + ids, method: 'delete' })
}
