<template>
  <div class="app-container calendar-list-container">

    <!-- 查询和其他操作 -->
    <div class="filter-container">
      <el-input v-model="listQuery.userId" clearable class="filter-item" style="width: 200px;" :placeholder="$t('agent_session.placeholder.filter_user_id')"/>
      <el-input v-model="listQuery.keyword" clearable class="filter-item" style="width: 200px;" :placeholder="$t('agent_session.placeholder.filter_keyword')"/>
      <el-button class="filter-item" type="primary" icon="Search" @click="handleFilter">{{ $t('app.button.search') }}</el-button>
    </div>

    <!-- 查询结果 -->
    <el-table stripe v-loading="listLoading" :data="list" :element-loading-text="$t('app.message.list_loading')" fit highlight-current-row>
      <el-table-column align="center" width="100px" :label="$t('agent_session.table.id')" prop="id" sortable />

      <el-table-column align="center" min-width="100px" :label="$t('agent_session.table.user_id')" prop="userId" />

      <el-table-column align="center" min-width="180px" :label="$t('agent_session.table.session_id')" prop="sessionId" show-overflow-tooltip />

      <el-table-column align="center" min-width="160px" :label="$t('agent_session.table.title')" prop="title" show-overflow-tooltip />

      <el-table-column align="center" min-width="100px" :label="$t('agent_session.table.turn_count')" prop="turnCount" />

      <el-table-column align="center" min-width="200px" :label="$t('agent_session.table.last_query')" prop="lastQuery" show-overflow-tooltip />

      <el-table-column align="center" min-width="140px" :label="$t('agent_session.table.model')" prop="model" show-overflow-tooltip />

      <el-table-column align="center" min-width="100px" :label="$t('agent_session.table.status')" prop="status" />

      <el-table-column align="center" min-width="160px" :label="$t('agent_session.table.add_time')" prop="addTime" />

      <el-table-column align="center" width="120px" :label="$t('agent_session.table.actions')">
        <template #default="scope">
          <el-button type="primary" size="small" @click="handleDetail(scope.row)">{{ $t('agent_session.button.detail') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="listQuery.page" v-model:limit="listQuery.limit" @pagination="getList" />

    <!-- 会话详情：阶段2D-5 对话时间线气泡（复用 H5 规范：用户红/助手白）+ 推荐商品卡 -->
    <el-dialog v-model="dialogVisible" :title="$t('agent_session.dialog.title')" width="70%">
      <div v-loading="dialogLoading" :element-loading-text="$t('app.message.list_loading')">
        <div class="agent-session-section-title">{{ $t('agent_session.dialog.messages_title') }}</div>
        <div class="chat-timeline">
          <div v-if="sortedMessages.length === 0" class="chat-empty">暂无消息</div>
          <div v-for="m in sortedMessages" :key="m.id" class="chat-row" :class="m.role === 'assistant' ? 'chat-row-ai' : 'chat-row-user'">
            <div class="chat-bubble">{{ m.content }}</div>
            <div v-if="m.role === 'assistant'" class="chat-meta">
              <span v-if="m.model">{{ m.model }}</span>
              <span v-if="m.latencyMs != null">{{ m.latencyMs }} ms</span>
              <el-tag v-if="m.degraded" type="warning" size="small">{{ $t('agent_session.value.degraded') }}</el-tag>
              <span>{{ m.addTime }}</span>
            </div>
            <div v-else class="chat-meta">{{ m.addTime }}</div>
          </div>
        </div>

        <div class="agent-session-section-title">{{ $t('agent_session.dialog.recommendations_title') }}</div>
        <div v-if="recommendList.length === 0" class="chat-empty">暂无推荐记录</div>
        <div v-else class="rec-grid">
          <div v-for="r in recommendList" :key="r.id" class="rec-card">
            <img v-if="r.goodsPic" :src="r.goodsPic" class="rec-pic" >
            <div v-else class="rec-pic rec-pic-fallback" />
            <div class="rec-body">
              <div class="rec-name">{{ r.goodsName }}</div>
              <div class="rec-line">
                <span class="rec-price">¥{{ r.retailPrice }}</span>
                <el-tag v-if="r.clicked" type="success" size="small">{{ $t('agent_session.value.clicked_yes') }}</el-tag>
                <el-tag v-else type="info" size="small">{{ $t('agent_session.value.clicked_no') }}</el-tag>
              </div>
              <div class="rec-meta">rank {{ r.rank }} · score {{ formatScore(r.score) }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import { listAgentSessions, listAgentMessages, listAgentRecommendations } from '@/api/agent'
import Pagination from '@/components/Pagination' // Secondary package based on el-pagination

export default {
  name: 'AgentSessions',
  components: { Pagination },
  data() {
    return {
      list: null,
      total: 0,
      listLoading: true,
      listQuery: {
        page: 1,
        limit: 20,
        userId: undefined,
        keyword: undefined,
        sort: 'add_time',
        order: 'desc'
      },
      dialogVisible: false,
      dialogLoading: false,
      messageList: [],
      recommendList: []
    }
  },
  created() {
    this.getList()
  },
  computed: {
    // 时间线按 id 升序（接口分页排序不保证对话顺序）
    sortedMessages() {
      return [...this.messageList].sort((a, b) => a.id - b.id)
    }
  },
  methods: {
    getList() {
      this.listLoading = true
      listAgentSessions(this.listQuery)
        .then(response => {
          this.list = response.data.data.list
          this.total = response.data.data.total
          this.listLoading = false
        })
        .catch(() => {
          this.list = []
          this.total = 0
          this.listLoading = false
        })
    },
    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
    handleDetail(row) {
      this.messageList = []
      this.recommendList = []
      this.dialogVisible = true
      this.dialogLoading = true
      const sessionId = row.sessionId
      Promise.all([
        listAgentMessages({ sessionId: sessionId, page: 1, limit: 100 }),
        listAgentRecommendations({ sessionId: sessionId, page: 1, limit: 100 })
      ])
        .then(results => {
          this.messageList = results[0].data.data.list
          this.recommendList = results[1].data.data.list
          this.dialogLoading = false
        })
        .catch(() => {
          this.messageList = []
          this.recommendList = []
          this.dialogLoading = false
        })
    },
    formatScore(value) {
      const num = Number(value)
      return isNaN(num) ? '-' : num.toFixed(4)
    }
  }
}
</script>

<style scoped>
.agent-session-section-title {
  margin: 16px 0 8px;
  font-weight: bold;
}

/* 阶段2D-5：对话时间线气泡（H5 规范：用户品牌红/助手白卡） */
.chat-timeline {
  max-height: 420px;
  overflow-y: auto;
  background: var(--bg, #f7f8fa);
  border-radius: 12px;
  padding: 16px;
}
.chat-row {
  display: flex;
  flex-direction: column;
  margin-bottom: 14px;
}
.chat-row-user {
  align-items: flex-end;
}
.chat-row-ai {
  align-items: flex-start;
}
.chat-bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.chat-row-user .chat-bubble {
  background: #d93a3a;
  color: #fff;
  border-top-right-radius: 4px;
}
.chat-row-ai .chat-bubble {
  background: #fff;
  color: var(--text-1, #1f2329);
  border-top-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(31, 35, 41, 0.04);
}
.chat-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-3, #86909c);
}
.chat-empty {
  padding: 24px;
  text-align: center;
  color: var(--text-3, #86909c);
}

/* 推荐商品卡 */
.rec-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}
.rec-card {
  display: flex;
  gap: 10px;
  background: #fff;
  border: 1px solid var(--line, #e5e6eb);
  border-radius: 12px;
  padding: 12px;
}
.rec-pic {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  object-fit: cover;
  flex: none;
}
.rec-pic-fallback {
  background: var(--bg, #f7f8fa);
}
.rec-body {
  min-width: 0;
  flex: 1;
}
.rec-name {
  font-size: 13px;
  color: var(--text-1, #1f2329);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.rec-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}
.rec-price {
  color: #d93a3a;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.rec-meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-3, #86909c);
}
</style>
