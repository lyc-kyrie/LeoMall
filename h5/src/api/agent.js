import request from '@/utils/request'

// 智能导购接口统一标记 silentError：由 AgentChat 组件按 errno 给出准确提示，
// 避免 501/506/超时被全局拦截器当成「登录连接超时」或统一显示「连不上服务」。
const silentError = true

// 智能导购 Agent 对话：新建/继续会话并获取推荐语与商品卡
// 聊天可能较慢，单独放开 axios 超时，避免默认 5000ms 被截断
export function agentChat(data) {
  return request({
    url: '/agent/chat',
    method: 'post',
    data,
    timeout: 60000,
    silentError
  })
}

// 会话列表（data 为数组）
export function agentSessions() {
  return request({
    url: '/agent/sessions',
    method: 'get',
    silentError
  })
}

// 某个会话的历史消息
export function agentHistory(params) {
  return request({
    url: '/agent/history',
    method: 'get',
    params,
    silentError
  })
}

// 清空/结束某个会话
export function agentClear(data) {
  return request({
    url: '/agent/clear',
    method: 'post',
    data,
    silentError
  })
}

// 推荐商品点击上报
export function agentClick(data) {
  return request({
    url: '/agent/click',
    method: 'post',
    data,
    silentError
  })
}
