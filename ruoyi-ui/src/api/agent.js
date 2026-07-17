import request from '@/utils/request'

export function listAgents(params) {
  return request({
    url: '/agent/manage/list',
    method: 'get',
    params
  })
}

export function addAgent(data) {
  return request({
    url: '/agent/manage',
    method: 'post',
    data
  })
}

export function resetAgentSecret(agentId) {
  return request({
    url: `/agent/manage/${agentId}/reset-secret`,
    method: 'post'
  })
}

export function updateAgent(agent) {
  return request({
    url: `/agent/manage/${agent.agentId}`,
    method: 'put',
    data: agent
  })
}
