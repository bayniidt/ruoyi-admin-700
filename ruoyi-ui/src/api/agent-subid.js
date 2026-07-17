import request from '@/utils/request'

export function getMySubIdList() {
  return request({
    url: '/agent/subid/list',
    method: 'get'
  })
}

export function getDownlineSubIdList() {
  return request({
    url: '/agent/subid/downline',
    method: 'get'
  })
}

export function createSubId(data) {
  return request({
    url: '/agent/subid',
    method: 'post',
    data
  })
}
