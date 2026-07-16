import request from '@/utils/request'

export function getPartnerStackCustomers(params) {
  return request({
    url: '/partnerstack/customers',
    method: 'get',
    params
  })
}

export function getPartnerStackActions(params) {
  return request({
    url: '/partnerstack/actions',
    method: 'get',
    params
  })
}

export function getPartnerStackTransactions(params) {
  return request({
    url: '/partnerstack/transactions',
    method: 'get',
    params
  })
}

export function getPartnerStackRewards(params) {
  return request({
    url: '/partnerstack/rewards',
    method: 'get',
    params
  })
}
