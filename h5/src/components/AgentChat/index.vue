<template>
  <div class="agent-chat">
    <!-- 悬浮球：固定右下角，避开 Tabbar -->
    <div class="agent-fab" v-show="!visible" @click="openPanel">
      <svg class="agent-fab-icon" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <path d="M12 3C6.6 3 2 6.7 2 11.2c0 2.3 1.2 4.3 3.1 5.7L4.4 21l4.2-1.9c1.1.3 2.2.5 3.4.5 5.4 0 10-3.7 10-8.4S17.4 3 12 3z" fill="#ffffff"/>
        <circle cx="7.8" cy="11.2" r="1.25"/>
        <circle cx="12" cy="11.2" r="1.25"/>
        <circle cx="16.2" cy="11.2" r="1.25"/>
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
            <button class="agent-btn" type="button" @click="openHist">历史</button>
            <button class="agent-btn" type="button" @click="newSession">新会话</button>
            <button class="agent-close" type="button" @click="closePanel">
              <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path d="M6 6l12 12M18 6L6 18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
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
                <goods-card
                  v-for="(goods, gi) in msg.recommendations"
                  :key="gi"
                  :goods="recItem(goods)"
                  @click="goDetail(goods)"
                />
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

    <!-- 历史会话侧栏抽屉（自绘固定层，层级高于聊天面板 2200） -->
    <transition name="agent-fade">
      <div class="agent-hist-mask" v-if="showHist" @click="showHist = false"></div>
    </transition>
    <transition name="agent-slide-right">
      <div class="agent-hist" v-if="showHist">
        <div class="agent-hist_head">
          <span>历史会话</span>
          <button class="agent-hist_close" type="button" @click="showHist = false">
            <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path d="M6 6l12 12M18 6L6 18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
        <div class="agent-hist_list">
          <div v-if="!sessions.length" class="agent-hist_empty">暂无历史会话</div>
          <div
            v-for="s in sessions"
            :key="s.sessionId"
            class="agent-hist_item"
            :class="{ 'is-active': s.sessionId === sessionId }"
            @click="switchSession(s)"
          >
            <div class="agent-hist_title">{{ s.title || s.lastQuery || '未命名会话' }}</div>
            <div class="agent-hist_meta">{{ s.addTime }} · {{ s.turnCount || 0 }} 轮</div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
// 阶段2D-3：气泡规范（用户红底白字/AI 白卡）+ 推荐卡复用 goods-card + 胶囊输入 + 历史侧栏抽屉
import { agentChat, agentSessions, agentHistory, agentClear, agentClick } from '@/api/agent'
import GoodsCard from '@/components/goods-card'

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
      showHist: false,
      sessions: []
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
  mounted() {
    // 「我的」页 AI 导购入口唤起
    window.addEventListener('leomall:agent-open', this.openPanel)
  },
  beforeUnmount() {
    window.removeEventListener('leomall:agent-open', this.openPanel)
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
    recItem(goods) {
      return {
        id: goods.goodsId,
        name: goods.name,
        picUrl: goods.picUrl,
        retailPrice: goods.retailPrice,
        counterPrice: goods.counterPrice
      }
    },
    openHist() {
      this.showHist = true
      agentSessions()
        .then(res => {
          this.sessions = (res.data && res.data.data) || []
        })
        .catch(() => {
          this.sessions = []
        })
    },
    switchSession(s) {
      this.sessionId = s.sessionId
      window.localStorage.setItem(STORAGE_KEY, s.sessionId)
      this.showHist = false
      this.restoreHistory(s.sessionId)
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
    }
  },
  components: {
    GoodsCard
  }
}
</script>

<style lang="scss" scoped>
// 阶段2D-3：同色相渐变（浅→深），气泡/输入/抽屉全部走 tokens
$agent-color: var(--brand);
$agent-color-light: #E4574F;
$agent-color-deep: var(--brand-deep);

.agent-fab {
  position: fixed;
  right: 14px;
  bottom: 70px;
  z-index: 2100;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: $agent-color;
  box-shadow: 0 6px 16px rgba(217, 58, 58, 0.35);
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
  circle {
    fill: var(--brand);
  }
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
  z-index: 2190;
  background: rgba(0, 0, 0, 0.38);
}
.agent-fade-enter-active,
.agent-fade-leave-active {
  transition: opacity 0.22s ease-out;
}
.agent-fade-enter-from,
.agent-fade-leave-to {
  opacity: 0;
}

.agent-panel {
  position: fixed;
  inset: 0;
  z-index: 2200;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  overflow: hidden;
}
.agent-up-enter-active,
.agent-up-leave-active {
  transition: transform 0.24s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.agent-up-enter-from,
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
  background: linear-gradient(135deg, $agent-color-light 0%, $agent-color-deep 100%);
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
  border-radius: var(--radius-pill);
  margin-right: 8px;
  line-height: 1.2;
}
.agent-close {
  border: none;
  background: transparent;
  color: #fff;
  width: 28px;
  height: 28px;
  padding: 4px;
  svg {
    width: 100%;
    height: 100%;
    display: block;
  }
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
  color: var(--text-3);
  background: rgba(31, 35, 41, 0.04);
  padding: 5px 12px;
  border-radius: var(--radius-pill);
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
  background: $agent-color;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.agent-bubble-assistant {
  background: var(--card);
  color: var(--text-1);
  border-bottom-left-radius: 4px;
  box-shadow: var(--shadow-1);
}

/* 推荐卡复用共用 goods-card：双列网格 */
.agent-recs {
  margin-top: 8px;
  width: 100%;
  max-width: 86%;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.agent-input-bar {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  padding: 8px 10px;
  padding-bottom: calc(8px + constant(safe-area-inset-bottom));
  padding-bottom: calc(8px + env(safe-area-inset-bottom));
  background: var(--card);
  border-top: 1px solid var(--line);
  box-sizing: border-box;
}
.agent-input {
  flex: 1 1 auto;
  min-width: 0;
  height: 38px;
  border: 1px solid transparent;
  border-radius: var(--radius-pill);
  background: var(--bg);
  padding: 0 14px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  &:focus {
    border-color: $agent-color;
    background: var(--card);
  }
}
.agent-send {
  flex: 0 0 auto;
  margin-left: 8px;
  height: 38px;
  padding: 0 16px;
  border: none;
  border-radius: var(--radius-pill);
  background: $agent-color;
  color: #fff;
  font-size: 14px;
  line-height: 38px;
  &:disabled {
    opacity: 0.6;
  }
}

/* 历史会话侧栏（自绘固定层） */
.agent-hist-mask {
  position: fixed;
  inset: 0;
  z-index: 2290;
  background: rgba(0, 0, 0, 0.38);
}
.agent-slide-right-enter-active,
.agent-slide-right-leave-active {
  transition: transform 0.24s ease-out;
}
.agent-slide-right-enter-from,
.agent-slide-right-leave-to {
  transform: translateX(100%);
}
.agent-hist {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 78%;
  z-index: 2300;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  border-radius: 16px 0 0 16px;
  box-shadow: var(--shadow-2);
  overflow: hidden;
}
.agent-hist_head {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: var(--card);
  font-size: 16px;
  font-weight: 600;
  color: var(--text-1);
}
.agent-hist_close {
  border: none;
  background: transparent;
  color: var(--text-3);
  width: 24px;
  height: 24px;
  padding: 2px;
  svg {
    width: 100%;
    height: 100%;
    display: block;
  }
}
.agent-hist_list {
  flex: 1 1 auto;
  overflow-y: auto;
  padding: 12px;
}
.agent-hist_empty {
  padding: 40px 0;
  text-align: center;
  font-size: 13px;
  color: var(--text-3);
}
.agent-hist_item {
  background: var(--card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-1);
  padding: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  &:active {
    opacity: 0.85;
  }
  &.is-active {
    background: var(--brand-soft);
  }
}
.agent-hist_title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.agent-hist_meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-3);
  font-variant-numeric: tabular-nums;
}
</style>