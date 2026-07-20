import request from '@/utils/request'

export function listTutorialFiles(query) {
  return request({
    url: '/tutorial/file/list',
    method: 'get',
    params: query
  })
}

export function uploadTutorialFile(data) {
  return request({
    url: '/tutorial/file/upload',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data: data
  })
}

export function delTutorialFile(fileId) {
  return request({
    url: '/tutorial/file/' + fileId,
    method: 'delete'
  })
}

export function previewTutorialFile(fileId) {
  return request({
    url: '/tutorial/file/preview/' + fileId,
    method: 'get',
    responseType: 'blob'
  })
}
