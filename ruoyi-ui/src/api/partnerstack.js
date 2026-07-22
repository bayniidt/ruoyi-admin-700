import request from '@/utils/request'

const PARTNERSTACK_TIMEOUT = 30000

function partnerStackGet(url, params) {
  return request({
    url,
    method: 'get',
    params,
    timeout: PARTNERSTACK_TIMEOUT
  })
}

export function getPartnerStackDashboard(params) {
  return partnerStackGet('/partnerstack/dashboard', params)
}

export function getPartnerStackCustomers(params) {
  return partnerStackGet('/partnerstack/customers', params)
}

export function getPartnerStackActions(params) {
  return partnerStackGet('/partnerstack/actions', params)
}

export function getPartnerStackTransactions(params) {
  return partnerStackGet('/partnerstack/transactions', params)
}

export function getPartnerStackAdAccounts(params) {
  return partnerStackGet('/partnerstack/ad-accounts', params)
}

export function getPartnerStackTransactionDetails(params) {
  return partnerStackGet('/partnerstack/transaction-details', params)
}

export function getPartnerStackRewards(params) {
  return partnerStackGet('/partnerstack/rewards', params)
}
