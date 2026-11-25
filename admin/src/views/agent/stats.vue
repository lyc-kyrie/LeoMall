<template>
  <div class="agent-stats">
    <!-- 阶段2D-5：KPI 卡复用仪表盘规范（浅色图标底块 + tabular 数字） -->
    <div class="stats-cards">
      <div v-for="c in cards" :key="c.key" class="stats-card">
        <div class="stats-card-icon" :style="{ background: c.soft, color: c.color }">
          <svg-icon :icon-class="c.icon" />
        </div>
        <div>
          <div class="stats-card-label">{{ $t('agent_stats.card.' + c.key) }}</div>
          <div class="stats-card-num">{{ c.value }}</div>
        </div>
      </div>
    </div>

    <div class="stats-bottom">
      <div class="stats-panel">
        <div class="stats-panel-title">推荐转化漏斗</div>
        <div ref="funnelChart" class="stats-funnel" />
      </div>
      <div class="stats-panel">
        <div class="stats-panel-title">{{ $t('agent_stats.section.top_goods') }}</div>
        <el-table v-loading="listLoading" stripe :data="topGoods" :element-loading-text="$t('app.message.list_loading')" fit>
          <el-table-column align="center" width="80px" :label="$t('agent_stats.table.rank')" type="index" />
          <el-table-column align="center" min-width="100px" :label="$t('agent_stats.table.goods_id')" prop="goodsId" />
          <el-table-column align="center" min-width="200px" :label="$t('agent_stats.table.goods_name')" prop="goodsName" show-overflow-tooltip />
          <el-table-column align="center" min-width="100px" :label="$t('agent_stats.table.count')" prop="count" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
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
      topGoods: [],
      funnelChart: null
    }
  },
  computed: {
    cards() {
      return [
        { key: 'session_count', icon: 'message', value: this.stats.sessionCount, color: '#1890ff', soft: '#e8f4ff' },
        { key: 'message_count', icon: 'form', value: this.stats.messageCount, color: '#22a06b', soft: '#e6f6ef' },
        { key: 'user_count', icon: 'peoples', value: this.stats.userCount, color: '#f5a623', soft: '#fdf3e2' },
        { key: 'recommend_count', icon: 'shopping', value: this.stats.recommendCount, color: '#7a5af8', soft: '#f0ebfe' },
        { key: 'click_count', icon: 'eye', value: this.stats.clickCount, color: '#e5484d', soft: '#fdecec' },
        { key: 'click_rate', icon: 'chart', value: this.formatClickRate(this.stats.clickRate), color: '#0fc6c2', soft: '#e6faf9' }
      ]
    }
  },
  mounted() {
    this.funnelChart = echarts.init(this.$refs.funnelChart)
    this.onResize = () => this.funnelChart.resize()
    window.addEventListener('resize', this.onResize)
    this.getStats()
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.onResize)
    this.funnelChart.dispose()
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
          this.renderFunnel()
        })
        .catch(() => {
          this.topGoods = []
          this.listLoading = false
        })
    },
    renderFunnel() {
      const rec = this.stats.recommendCount || 0
      const click = this.stats.clickCount || 0
      if (rec === 0 && click === 0) {
        this.funnelChart.setOption({ graphic: { type: 'text', left: 'center', top: 'middle', style: { text: '暂无推荐数据', fill: '#86909c', fontSize: 13 } } })
        return
      }
      this.funnelChart.setOption({
        color: ['#1890ff', '#22a06b'],
        tooltip: { trigger: 'item', formatter: '{b}: {c}' },
        series: [{
          type: 'funnel',
          left: 24,
          right: 24,
          top: 16,
          bottom: 16,
          minSize: '24%',
          gap: 4,
          label: { show: true, position: 'inside', formatter: '{b}\n{c}', color: '#fff', fontSize: 13 },
          itemStyle: { borderRadius: 4 },
          data: [
            { name: '推荐曝光', value: rec },
            { name: '用户点击', value: click }
          ]
        }]
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
.agent-stats {
  padding: 20px;
  background: var(--bg, #f7f8fa);
  min-height: calc(100vh - 84px);
}
.stats-cards {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
}
.stats-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 2px rgba(31, 35, 41, 0.04);
}
.stats-card-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex: none;
}
.stats-card-label {
  font-size: 12px;
  color: var(--text-3, #86909c);
  margin-bottom: 4px;
  white-space: nowrap;
}
.stats-card-num {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-1, #1f2329);
  font-variant-numeric: tabular-nums;
  line-height: 1;
}
.stats-bottom {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 16px;
  margin-top: 16px;
}
.stats-panel {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 1px 2px rgba(31, 35, 41, 0.04);
}
.stats-panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1, #1f2329);
  margin-bottom: 10px;
}
.stats-funnel {
  width: 100%;
  height: 280px;
}
@media (max-width: 1400px) {
  .stats-cards { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 1200px) {
  .stats-bottom { grid-template-columns: 1fr; }
}
</style>