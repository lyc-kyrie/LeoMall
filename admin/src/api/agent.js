import request from '@/utils/request'

export function listAgentSessions(query) {
  return request({
    url: '/agent/sessions',
    method: 'get',
    params: query
  })
}

export function listAgentMessages(query) {
  return request({
    url: '/agent/messages',
    method: 'get',
    params: query
  })
}

export function listAgentRecommendations(query) {
  return request({
    url: '/agent/recommendations',
    method: 'get',
    params: query
  })
}

export function getAgentStats() {
  return request({
    url: '/agent/stats',
    method: 'get'
  })
}
