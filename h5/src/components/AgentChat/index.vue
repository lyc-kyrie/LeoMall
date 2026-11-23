<template>
  <div class="agent-chat">
    <!-- 悬浮球：固定右下角，避开 Tabbar -->
    <div class="agent-fab" v-show="!visible" @click="openPanel">
      <svg class="agent-fab-icon" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <path d="M12 3C6.6 3 2 6.7 2 11.2c0 2.3 1.2 4.3 3.1 5.7L4.4 21l4.2-1.9c1.1.3 2.2.5 3.4.5 5.4 0 10-3.7 10-8.4S17.4 3 12 3z" fill="#ffffff"/>
        <circle cx="7.8" cy="11.2" r="1.25" fill="#ff5a2d"/>
        <circle cx="12" cy="11.2" r="1.25" fill="#ff5a2d"/>
        <circle cx="16.2" cy="11.2" r="1.25" fill="#ff5a2d"/>
      </svg>
      <span class="agent-fab-text">AI</span>
    </div>

    <!-- 遮罩 -->
    <transition name="agent-fade">
      <div class="agent-mask" v-if="visible" @click="closePanel"></div>
    </transition>

    <!-- 抽屉式聊天面板 -->
    <transition name="agent-up">
      <div class="agent-panel" v-if="visible">
        <div class="agent-header">
          <span class="agent-title">智能导购</span>
          <div class="agent-header-actions">
            <button class="agent-btn" type="button" @click="newSession">新会话</button>
            <button class="agent-close" type="button" @click="closePanel">&times;</button>
          </div>
        </div>

        <div class="agent-messages" ref="msgList">
          <div
            class="agent-msg-row"
            :class="'is-' + msg.role"
            v-for="(msg, index) in messages"
            :key="index"
          >
            <div v-if="msg.role === 'system'" class="agent-system">{{ msg.content }}</div>
            <template v-else>
              <div
                class="agent-bubble"
                :class="msg.role === 'user' ? 'agent-bubble-user' : 'agent-bubble-assistant'"
              >{{ msg.content }}</div>
              <div
                class="agent-recs"
                v-if="msg.role === 'assistant' && msg.recommendations && msg.recommendations.length"
              >
                <div
                  class="agent-rec-card"
                  v-for="(goods, gi) in msg.recommendations"
                  :key="gi"
                  @click="goDetail(goods)"
                >
                  <img class="agent-rec-img" :src="goods.picUrl" @error="onImgError" />
                  <div class="agent-rec-info">
                    <div class="agent-rec-name">{{ goods.name }}</div>
                    <div class="agent-rec-brief">{{ goods.brief }}</div>
                    <div class="agent-rec-price">
                      <span class="agent-rec-now">&yen;{{ fmtPrice(goods.retailPrice) }}</span>
                      <span class="agent-rec-old" v-if="goods.counterPrice">&yen;{{ fmtPrice(goods.counterPrice) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>

        <div class="agent-input-bar">
          <input
            class="agent-input"
            v-model="inputText"
            type="text"
            placeholder="说说你想买点什么…"
            @keyup.enter="send"
          />
          <button class="agent-send" type="button" :disabled="sending" @click="send">
            {{ sending ? '思考中…' : '发送' }}
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { agentChat, agentHistory, agentClear, agentClick } from '@/api/agent'

const STORAGE_KEY = 'agentSessionId'

export default {
  name: 'AgentChat',
  data() {
    return {
      visible: false,
      messages: [],
      inputText: '',
      sending: false,
      sessionId: '',
      defaultGoodsImg: require('@/assets/images/goods_default.png')
    }
  },
  created() {
    // 已有会话且已登录才还原历史；未登录时不发无效请求
    const sid = window.localStorage.getItem(STORAGE_KEY)
    if (sid && window.localStorage.getItem('Authorization')) {
      this.sessionId = sid
      this.restoreHistory(sid)
    }
  },
  methods: {
    openPanel() {
      this.visible = true
      if (this.messages.length === 0) {
        this.pushWelcome()
      }
      this.scrollToBottom()
    },
    closePanel() {
      this.visible = false
    },
    pushWelcome() {
      this.messages.push({
        role: 'assistant',
        content: '你好呀～我是你的智能导购。告诉我你想买什么、预算多少，我来帮你挑好物！',
        recommendations: []
      })
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const box = this.$refs.msgList
        if (box) {
          box.scrollTop = box.scrollHeight
        }
      })
    },
    restoreHistory(sid) {
      agentHistory({ sessionId: sid })
        .then(res => {
          const data = res.data.data
          const list = data && data.messages ? data.messages : []
          this.messages = list.map(m => ({
            id: m.id,
            role: m.role,
            content: m.content || '',
            recommendations: m.recommendations || [],
            addTime: m.addTime
          }))
          this.scrollToBottom()
        })
        .catch(e => {
          // 会话已失效（506）：清掉本地残留，后续提问会自动开启新会话
          if (this.isStaleSession(e)) {
            window.localStorage.removeItem(STORAGE_KEY)
            this.sessionId = ''
          }
        })
    },
    pushSystem(content) {
      this.messages.push({ role: 'system', content, recommendations: [] })
    },
    errorBody(e) {
      if (!e) return {}
      return e.data || (e.response && e.response.data) || {}
    },
    isStaleSession(e) {
      return this.errorBody(e).errno === 506
    },
    errorMessage(e) {
      const body = this.errorBody(e)
      if (body.errno === 501) return '登录状态已失效，请重新登录后再咨询～'
      if (body.errno === 506) return '上一轮会话已失效，请点「新会话」后重新提问～'
      if (body.errno === 502) return body.errmsg || '导购服务暂时不可用，请稍后再试～'
      const timedOut = e && (e.code === 'ECONNABORTED' || /timeout/i.test(e.message || ''))
      if (timedOut) return '导购服务响应有点慢，请稍后再试一次～'
      if (body.errmsg) return body.errmsg
      return '网络连接异常，请检查网络后重试～'
    },
    async send() {
      const text = this.inputText.trim()
      if (!text || this.sending) return

      // 未登录：不发请求，直接给出准确引导（避免 501 被误报成服务故障）
      if (!window.localStorage.getItem('Authorization')) {
        this.pushSystem('你还没有登录，请先登录后就能和导购对话啦～（点底部「我的」进入登录，输入内容已保留）')
        this.scrollToBottom()
        return
      }

      this.inputText = ''
      this.messages.push({ role: 'user', content: text, recommendations: [] })
      this.scrollToBottom()
      this.sending = true
      try {
        // 浏览器可能残留失效或属于其他账号的会话号：清掉后自动用新会话重试一次
        for (let attempt = 0; attempt < 2; attempt++) {
          try {
            const res = await agentChat({ sessionId: this.sessionId, query: text })
            const data = (res && res.data && res.data.data) || {}
            if (data.sessionId) {
              this.sessionId = data.sessionId
              window.localStorage.setItem(STORAGE_KEY, data.sessionId)
            }
            this.messages.push({
              role: 'assistant',
              content: data.reply || '抱歉，我暂时没能组织好语言，请再问我一次～',
              recommendations: data.recommendations || []
            })
            break
          } catch (e) {
            if (attempt === 0 && this.isStaleSession(e)) {
              window.localStorage.removeItem(STORAGE_KEY)
              this.sessionId = ''
              continue
            }
            this.pushSystem(this.errorMessage(e))
          }
        }
      } finally {
        this.sending = false
        this.scrollToBottom()
      }
    },
    async newSession() {
      const oldSid = this.sessionId
      window.localStorage.removeItem(STORAGE_KEY)
      this.sessionId = ''
      this.messages = []
      this.inputText = ''
      this.sending = false
      this.pushWelcome()
      this.scrollToBottom()
      if (oldSid) {
        try {
          await agentClear({ sessionId: oldSid })
        } catch (e) {
          // 清会话失败静默忽略
        }
      }
    },
    async goDetail(goods) {
      // 有 recommendationId 才做点击上报，且失败不影响跳转
      if (goods.recommendationId !== undefined && goods.recommendationId !== null) {
        try {
          await agentClick({ recommendationId: goods.recommendationId })
        } catch (e) {
          // 上报失败忽略
        }
      }
      this.$router.push({ name: 'detail', params: { itemId: String(goods.goodsId) } })
    },
    onImgError(e) {
      const el = e.target
      if (el.dataset.fallbacked) return
      el.dataset.fallbacked = '1'
      el.src = this.defaultGoodsImg
    },
    fmtPrice(value) {
      const n = Number(value)
      return isNaN(n) ? '0.00' : n.toFixed(2)
    }
  }
}
</script>

<style lang="scss" scoped>
$agent-color: #ff5a2d;
$agent-color-light: #ff8a3d;

.agent-fab {
  position: fixed;
  right: 14px;
  bottom: 70px;
  z-index: 2100;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, $agent-color-light 0%, $agent-color 100%);
  box-shadow: 0 6px 16px rgba(255, 90, 45, 0.45);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
  &:active {
    transform: scale(0.94);
  }
}
.agent-fab-icon {
  width: 22px;
  height: 22px;
}
.agent-fab-text {
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
  margin-top: 1px;
  letter-spacing: 0.5px;
}

.agent-mask {
  position: fixed;
  inset: 0;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 2190;
  background: rgba(0, 0, 0, 0.38);
}
.agent-fade-enter-active,
.agent-fade-leave-active {
  transition: opacity 0.28s ease;
}
.agent-fade-enter,
.agent-fade-leave-to {
  opacity: 0;
}

.agent-panel {
  position: fixed;
  inset: 0;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 2200;
  display: flex;
  flex-direction: column;
  background: #f5f6f8;
  overflow: hidden;
}
.agent-up-enter-active,
.agent-up-leave-active {
  transition: transform 0.3s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.agent-up-enter,
.agent-up-leave-to {
  transform: translateY(100%);
}

.agent-header {
  flex: 0 0 auto;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px 0 16px;
  background: linear-gradient(135deg, $agent-color-light 0%, $agent-color 100%);
  color: #fff;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.08);
  box-sizing: border-box;
}
.agent-title {
  font-size: 17px;
  font-weight: 600;
}
.agent-header-actions {
  display: flex;
  align-items: center;
}
.agent-btn {
  border: 1px solid rgba(255, 255, 255, 0.75);
  background: transparent;
  color: #fff;
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 14px;
  margin-right: 12px;
  line-height: 1.2;
}
.agent-close {
  border: none;
  background: transparent;
  color: #fff;
  font-size: 26px;
  line-height: 1;
  width: 28px;
  height: 28px;
  padding: 0;
}

.agent-messages {
  flex: 1 1 auto;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 14px 12px 18px;
  box-sizing: border-box;
}
.agent-msg-row {
  margin-bottom: 16px;
}
.agent-msg-row.is-user {
  display: flex;
  justify-content: flex-end;
}
.agent-msg-row.is-assistant {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.agent-msg-row.is-system {
  display: flex;
  justify-content: center;
}

.agent-system {
  font-size: 12px;
  color: #999;
  background: rgba(0, 0, 0, 0.04);
  padding: 5px 12px;
  border-radius: 12px;
  max-width: 80%;
  text-align: center;
  line-height: 1.5;
}

.agent-bubble {
  max-width: 78%;
  font-size: 14px;
  line-height: 1.6;
  padding: 9px 13px;
  border-radius: 14px;
  word-break: break-word;
  white-space: pre-wrap;
  box-sizing: border-box;
}
.agent-bubble-user {
  background: linear-gradient(135deg, $agent-color-light 0%, $agent-color 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.agent-bubble-assistant {
  background: #fff;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.agent-recs {
  margin-top: 8px;
  width: 100%;
  max-width: 86%;
}
.agent-rec-card {
  display: flex;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 8px;
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.06);
  padding: 8px;
  box-sizing: border-box;
  &:active {
    opacity: 0.85;
  }
}
.agent-rec-img {
  flex: 0 0 auto;
  width: 72px;
  height: 72px;
  border-radius: 6px;
  object-fit: cover;
  background: #f2f2f2;
}
.agent-rec-info {
  flex: 1 1 auto;
  min-width: 0;
  margin-left: 10px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.agent-rec-name {
  font-size: 13px;
  color: #333;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.agent-rec-brief {
  font-size: 11px;
  color: #999;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  margin: 4px 0;
}
.agent-rec-price {
  display: flex;
  align-items: baseline;
}
.agent-rec-now {
  color: $agent-color;
  font-size: 15px;
  font-weight: 600;
}
.agent-rec-old {
  color: #bbb;
  font-size: 11px;
  text-decoration: line-through;
  margin-left: 8px;
}

.agent-input-bar {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  padding: 8px 10px;
  padding-bottom: calc(8px + constant(safe-area-inset-bottom));
  padding-bottom: calc(8px + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid #eee;
  box-sizing: border-box;
}
.agent-input {
  flex: 1 1 auto;
  min-width: 0;
  height: 38px;
  border: 1px solid #e5e5e5;
  border-radius: 19px;
  background: #f7f7f7;
  padding: 0 14px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  &:focus {
    border-color: $agent-color-light;
    background: #fff;
  }
}
.agent-send {
  flex: 0 0 auto;
  margin-left: 8px;
  height: 38px;
  padding: 0 16px;
  border: none;
  border-radius: 19px;
  background: linear-gradient(135deg, $agent-color-light 0%, $agent-color 100%);
  color: #fff;
  font-size: 14px;
  line-height: 38px;
  &:disabled {
    opacity: 0.6;
  }
}
</style>
