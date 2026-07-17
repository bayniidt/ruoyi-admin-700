import request from '@/utils/request'

export function getPartnerStackDashboard(params) {
  return request({
    url: '/partnerstack/dashboard',
    method: 'get',
    params
  })
}

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

export function getPartnerStackAdAccounts(params) {
  return request({
    url: '/partnerstack/ad-accounts',
    method: 'get',
    params
  })
}

export function getPartnerStackTransactionDetails(params) {
  return request({
    url: '/partnerstack/transaction-details',
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
