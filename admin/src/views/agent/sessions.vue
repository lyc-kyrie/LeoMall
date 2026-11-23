<template>
  <div class="app-container calendar-list-container">

    <!-- 查询和其他操作 -->
    <div class="filter-container">
      <el-input v-model="listQuery.userId" clearable class="filter-item" style="width: 200px;" :placeholder="$t('agent_session.placeholder.filter_user_id')"/>
      <el-input v-model="listQuery.keyword" clearable class="filter-item" style="width: 200px;" :placeholder="$t('agent_session.placeholder.filter_keyword')"/>
      <el-button class="filter-item" type="primary" icon="el-icon-search" @click="handleFilter">{{ $t('app.button.search') }}</el-button>
    </div>

    <!-- 查询结果 -->
    <el-table v-loading="listLoading" :data="list" :element-loading-text="$t('app.message.list_loading')" border fit highlight-current-row>
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
        <template slot-scope="scope">
          <el-button type="primary" size="mini" @click="handleDetail(scope.row)">{{ $t('agent_session.button.detail') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="listQuery.page" :limit.sync="listQuery.limit" @pagination="getList" />

    <!-- 会话详情 -->
    <el-dialog :visible.sync="dialogVisible" :title="$t('agent_session.dialog.title')" width="70%">
      <div v-loading="dialogLoading" :element-loading-text="$t('app.message.list_loading')">
        <div class="agent-session-section-title">{{ $t('agent_session.dialog.messages_title') }}</div>
        <el-table :data="messageList" border fit max-height="300">
          <el-table-column align="center" min-width="100px" :label="$t('agent_session.table.role')">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.role === 'assistant'" type="success">{{ $t('agent_session.value.role_assistant') }}</el-tag>
              <el-tag v-else>{{ $t('agent_session.value.role_user') }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column align="center" min-width="300px" :label="$t('agent_session.table.content')" prop="content" show-overflow-tooltip />

          <el-table-column align="center" min-width="140px" :label="$t('agent_session.table.model')" prop="model" show-overflow-tooltip />

          <el-table-column align="center" min-width="120px" :label="$t('agent_session.table.latency_ms')">
            <template slot-scope="scope">
              {{ scope.row.latencyMs }} ms
            </template>
          </el-table-column>

          <el-table-column align="center" min-width="100px" :label="$t('agent_session.table.degraded')">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.degraded" type="warning">{{ $t('agent_session.value.degraded') }}</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>

          <el-table-column align="center" min-width="160px" :label="$t('agent_session.table.add_time')" prop="addTime" />
        </el-table>

        <div class="agent-session-section-title">{{ $t('agent_session.dialog.recommendations_title') }}</div>
        <el-table :data="recommendList" border fit max-height="300">
          <el-table-column align="center" width="80px" :label="$t('agent_session.table.goods_pic')">
            <template slot-scope="scope">
              <img v-if="scope.row.goodsPic" :src="scope.row.goodsPic" width="40" height="40" >
              <span v-else>-</span>
            </template>
          </el-table-column>

          <el-table-column align="center" min-width="200px" :label="$t('agent_session.table.goods_name')" prop="goodsName" show-overflow-tooltip />

          <el-table-column align="center" min-width="120px" :label="$t('agent_session.table.retail_price')">
            <template slot-scope="scope">
              ¥{{ scope.row.retailPrice }}
            </template>
          </el-table-column>

          <el-table-column align="center" min-width="120px" :label="$t('agent_session.table.score')">
            <template slot-scope="scope">
              {{ formatScore(scope.row.score) }}
            </template>
          </el-table-column>

          <el-table-column align="center" min-width="100px" :label="$t('agent_session.table.rank')" prop="rank" />

          <el-table-column align="center" min-width="100px" :label="$t('agent_session.table.clicked')">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.clicked" type="success">{{ $t('agent_session.value.clicked_yes') }}</el-tag>
              <el-tag v-else type="info">{{ $t('agent_session.value.clicked_no') }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
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
</style>
