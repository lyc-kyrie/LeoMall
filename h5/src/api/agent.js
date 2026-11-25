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

// 流式对话（2026-11-25）：SSE 逐 token 回调 onDelta(token)，resolve 完整 {errno, data}。
// 用 fetch + ReadableStream 而非 EventSource：需要自定义认证头 X-Leomall-Token，
// 且 EventSource 只支持 GET。协议：delta / done / error 三种事件。
export async function agentChatStream(data, onDelta) {
  const base = import.meta.env.VITE_BASE_API || '/leomall/wx'
  const res = await fetch(base + '/agent/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Leomall-Token': window.localStorage.getItem('Authorization') || ''
    },
    body: JSON.stringify(data)
  })
  if (!res.ok || !res.body) {
    const err = new Error('stream unavailable')
    err.httpStatus = res.status
    throw err
  }
  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buf = ''
  let final = null
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buf += decoder.decode(value, { stream: true })
    let sep
    while ((sep = buf.indexOf('\n\n')) >= 0) {
      const block = buf.slice(0, sep)
      buf = buf.slice(sep + 2)
      let event = 'message'
      let dataStr = ''
      for (const line of block.split('\n')) {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        else if (line.startsWith('data:')) dataStr += line.slice(5).trim()
      }
      if (!dataStr) continue
      const payload = JSON.parse(dataStr)
      if (event === 'delta') {
        if (payload.token) onDelta(payload.token)
      } else if (event === 'done') {
        final = payload
      } else if (event === 'error') {
        const err = new Error(payload.errmsg || '智能助手暂时不可用')
        err.errno = payload.errno
        throw err
      }
    }
  }
  if (!final) throw new Error('stream ended without result')
  if (final.errno !== 0) {
    const err = new Error(final.errmsg || '智能助手暂时不可用')
    err.errno = final.errno
    throw err
  }
  return final
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
