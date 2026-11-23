<template>
  <div class="app-container">

    <div class="agent-stats-title">{{ $t('agent_stats.title') }}</div>

    <el-row :gutter="20">
      <el-col :span="4">
        <div class="agent-stats-card">
          <div class="agent-stats-card-label">{{ $t('agent_stats.card.session_count') }}</div>
          <div class="agent-stats-card-value">{{ stats.sessionCount }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="agent-stats-card">
          <div class="agent-stats-card-label">{{ $t('agent_stats.card.message_count') }}</div>
          <div class="agent-stats-card-value">{{ stats.messageCount }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="agent-stats-card">
          <div class="agent-stats-card-label">{{ $t('agent_stats.card.user_count') }}</div>
          <div class="agent-stats-card-value">{{ stats.userCount }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="agent-stats-card">
          <div class="agent-stats-card-label">{{ $t('agent_stats.card.recommend_count') }}</div>
          <div class="agent-stats-card-value">{{ stats.recommendCount }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="agent-stats-card">
          <div class="agent-stats-card-label">{{ $t('agent_stats.card.click_count') }}</div>
          <div class="agent-stats-card-value">{{ stats.clickCount }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="agent-stats-card">
          <div class="agent-stats-card-label">{{ $t('agent_stats.card.click_rate') }}</div>
          <div class="agent-stats-card-value">{{ formatClickRate(stats.clickRate) }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="agent-stats-section-title">{{ $t('agent_stats.section.top_goods') }}</div>
    <el-table v-loading="listLoading" :data="topGoods" :element-loading-text="$t('app.message.list_loading')" border fit highlight-current-row>
      <el-table-column align="center" width="100px" :label="$t('agent_stats.table.rank')" type="index" />

      <el-table-column align="center" min-width="120px" :label="$t('agent_stats.table.goods_id')" prop="goodsId" />

      <el-table-column align="center" min-width="200px" :label="$t('agent_stats.table.goods_name')" prop="goodsName" show-overflow-tooltip />

      <el-table-column align="center" min-width="120px" :label="$t('agent_stats.table.count')" prop="count" />
    </el-table>

  </div>
</template>

<script>
import { getAgentStats } from '@/api/agent'

export default {
  name: 'AgentStats',
  data() {
    return {
      listLoading: true,
      stats: {
        sessionCount: 0,
        messageCount: 0,
        userCount: 0,
        recommendCount: 0,
        clickCount: 0,
        clickRate: 0
      },
      topGoods: []
    }
  },
  created() {
    this.getStats()
  },
  methods: {
    getStats() {
      this.listLoading = true
      getAgentStats()
        .then(response => {
          const data = response.data.data
          this.stats = {
            sessionCount: data.sessionCount,
            messageCount: data.messageCount,
            userCount: data.userCount,
            recommendCount: data.recommendCount,
            clickCount: data.clickCount,
            clickRate: data.clickRate
          }
          this.topGoods = data.topGoods || []
          this.listLoading = false
        })
        .catch(() => {
          this.topGoods = []
          this.listLoading = false
        })
    },
    formatClickRate(value) {
      const rate = Number(value)
      if (isNaN(rate)) {
        return '0.00%'
      }
      const percent = rate > 0 && rate <= 1 ? rate * 100 : rate
      return percent.toFixed(2) + '%'
    }
  }
}
</script>

<style scoped>
.agent-stats-title {
  margin-bottom: 20px;
  font-size: 18px;
  font-weight: bold;
}
.agent-stats-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 20px 10px;
  text-align: center;
}
.agent-stats-card-label {
  color: #909399;
  font-size: 14px;
}
.agent-stats-card-value {
  margin-top: 10px;
  font-size: 24px;
  font-weight: bold;
}
.agent-stats-section-title {
  margin: 24px 0 12px;
  font-size: 16px;
  font-weight: bold;
}
</style>
