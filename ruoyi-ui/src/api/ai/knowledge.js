import request from '@/utils/request'

// 上传 Word 文档
export function uploadDoc(formData) {
  return request({
    url: '/ai/knowledge/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 文档列表
export function listDoc(query) {
  return request({ url: '/ai/knowledge/list', method: 'get', params: query })
}

// 文档详情
export function getDoc(id) {
  return request({ url: '/ai/knowledge/' + id, method: 'get' })
}

// 删除文档
export function delDoc(id) {
  return request({ url: '/ai/knowledge/' + id, method: 'delete' })
}
