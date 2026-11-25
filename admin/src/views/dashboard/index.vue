<template>
  <div class="dash">
    <div class="dash-cards">
      <div v-for="c in cards" :key="c.key" class="dash-card">
        <div class="dash-card-icon" :style="{ background: c.soft, color: c.color }">
          <svg-icon :icon-class="c.icon" />
        </div>
        <div class="dash-card-body">
          <div class="dash-card-label">{{ $t('dashboard.section.' + c.key) }}</div>
          <div class="dash-card-num">{{ format(c.value) }}</div>
        </div>
      </div>
    </div>

    <div class="dash-charts">
      <div class="dash-chart-card">
        <div class="dash-chart-title">近 30 日订单趋势</div>
        <div ref="orderChart" class="dash-chart" />
      </div>
      <div class="dash-chart-card">
        <div class="dash-chart-title">近 30 日商品趋势</div>
        <div ref="goodsChart" class="dash-chart" />
      </div>
    </div>

    <!-- 阶段2D-5：待办卡 + AI 导购入口 + 最近订单（按权限显隐） -->
    <div class="dash-bottom">
      <div class="dash-side">
        <div v-if="canOrder || canAftersale" class="dash-panel">
          <div class="dash-panel-title">待办事项</div>
          <div v-if="canOrder" class="dash-todo-row" @click="$router.push('/mall/order')">
            <span class="dash-todo-dot" style="background:#f5a623" />
            <span class="dash-todo-label">待发货订单</span>
            <span class="dash-todo-num">{{ todoShip }}</span>
            <el-icon class="dash-todo-arrow"><ArrowRight /></el-icon>
          </div>
          <div v-if="canAftersale" class="dash-todo-row" @click="$router.push('/mall/aftersale')">
            <span class="dash-todo-dot" style="background:#e5484d" />
            <span class="dash-todo-label">待审核售后</span>
            <span class="dash-todo-num">{{ todoAftersale }}</span>
            <el-icon class="dash-todo-arrow"><ArrowRight /></el-icon>
          </div>
        </div>

        <div v-if="canAgent" class="dash-panel dash-ai" @click="$router.push('/agent/stats')">
          <div class="dash-panel-title">AI 导购</div>
          <div class="dash-ai-body">
            <div>
              <div class="dash-ai-num">{{ agentStats.sessionCount }}</div>
              <div class="dash-ai-label">累计会话</div>
            </div>
            <div>
              <div class="dash-ai-num">{{ formatRate(agentStats.clickRate) }}</div>
              <div class="dash-ai-label">推荐点击率</div>
            </div>
            <el-icon class="dash-todo-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <div v-if="canOrder" class="dash-panel dash-recent">
        <div class="dash-panel-title">
          最近订单
          <el-button link type="primary" class="dash-more" @click="$router.push('/mall/order')">查看全部</el-button>
        </div>
        <el-table stripe :data="recentOrders" :show-header="true">
          <el-table-column prop="orderSn" label="订单号" min-width="150" show-overflow-tooltip />
          <el-table-column prop="consignee" label="收货人" min-width="90" show-overflow-tooltip />
          <el-table-column label="实付" min-width="90" align="right">
            <template #default="scope"><span class="dash-price">¥{{ scope.row.actualPrice }}</span></template>
          </el-table-column>
          <el-table-column label="状态" min-width="90" align="center">
            <template #default="scope"><el-tag size="small">{{ statusText(scope.row.orderStatus) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="addTime" label="下单时间" min-width="150" show-overflow-tooltip />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
// 阶段2D-4c：vue-count-to 移除（数字直出 + tabular-nums），图表 echarts5 直连
// 阶段2D-5：待办卡（待发货/待售后）+ 最近订单 + AI 导购入口，复用 KPI 规范
import * as echarts from 'echarts'
import { info } from '@/api/dashboard'
import { statOrder, statGoods } from '@/api/stat'
import { listOrder } from '@/api/order'
import { listAftersale } from '@/api/aftersale'
import { getAgentStats } from '@/api/agent'
import checkPermission from '@/utils/permission'

const statusMap = {
  101: '未付款', 102: '用户取消', 103: '系统取消',
  201: '待发货', 202: '申请退款', 203: '已退款',
  301: '已发货', 401: '用户收货', 402: '系统收货'
}

export default {
  name: 'Dashboard',
  data() {
    return {
      userTotal: 0,
      goodsTotal: 0,
      productTotal: 0,
      orderTotal: 0,
      orderChart: null,
      goodsChart: null,
      canOrder: checkPermission(['GET /leomall/admin/order/list']),
      canAftersale: checkPermission(['GET /leomall/admin/aftersale/list']),
      canAgent: checkPermission(['GET /leomall/admin/agent/stats']),
      todoShip: 0,
      todoAftersale: 0,
      recentOrders: [],
      agentStats: { sessionCount: 0, clickRate: 0 }
    }
  },
  computed: {
    cards() {
      return [
        { key: 'user_total', icon: 'peoples', value: this.userTotal, color: '#1890ff', soft: '#e8f4ff' },
        { key: 'goods_total', icon: 'message', value: this.goodsTotal, color: '#22a06b', soft: '#e6f6ef' },
        { key: 'product_total', icon: 'shopping', value: this.productTotal, color: '#f5a623', soft: '#fdf3e2' },
        { key: 'order_total', icon: 'money', value: this.orderTotal, color: '#e5484d', soft: '#fdecec' }
      ]
    }
  },
  mounted() {
    this.orderChart = echarts.init(this.$refs.orderChart)
    this.goodsChart = echarts.init(this.$refs.goodsChart)
    this.onResize = () => {
      this.orderChart.resize()
      this.goodsChart.resize()
    }
    window.addEventListener('resize', this.onResize)

    info().then(response => {
      const d = response.data.data
      this.userTotal = d.userTotal
      this.goodsTotal = d.goodsTotal
      this.productTotal = d.productTotal
      this.orderTotal = d.orderTotal
    })

    statOrder().then(response => {
      const { columns, rows } = response.data.data
      this.orderChart.setOption(this.lineOption(rows, columns, { orders: '订单量', customers: '下单用户' }, '#1890ff'))
    })
    statGoods().then(response => {
      const { columns, rows } = response.data.data
      this.goodsChart.setOption(this.lineOption(rows, columns, { products: '下单货品数量', orders: '订单量' }, '#22a06b'))
    })

    if (this.canOrder) {
      listOrder({ page: 1, limit: 5, sort: 'add_time', order: 'desc' }).then(response => {
        this.recentOrders = response.data.data.list || []
      }).catch(() => {})
      listOrder({ page: 1, limit: 1, orderStatusArray: [201], sort: 'add_time', order: 'desc' }).then(response => {
        this.todoShip = response.data.data.total || 0
      }).catch(() => {})
    }
    if (this.canAftersale) {
      listAftersale({ page: 1, limit: 1, status: 1 }).then(response => {
        this.todoAftersale = response.data.data.total || 0
      }).catch(() => {})
    }
    if (this.canAgent) {
      getAgentStats().then(response => {
        const d = response.data.data
        this.agentStats = { sessionCount: d.sessionCount || 0, clickRate: d.clickRate || 0 }
      }).catch(() => {})
    }
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.onResize)
    this.orderChart.dispose()
    this.goodsChart.dispose()
  },
  methods: {
    format(n) {
      return (n || 0).toLocaleString('zh-CN')
    },
    statusText(status) {
      return statusMap[status] || status
    },
    formatRate(value) {
      const rate = Number(value)
      if (isNaN(rate)) return '0.00%'
      const percent = rate > 0 && rate <= 1 ? rate * 100 : rate
      return percent.toFixed(2) + '%'
    },
    lineOption(rows, columns, labels, color) {
      // 空数据给居中提示，避免裸坐标轴看起来像渲染失败
      if (!rows || rows.length === 0) {
        return { graphic: { type: 'text', left: 'center', top: 'middle', style: { text: '近 30 日暂无数据', fill: '#86909c', fontSize: 13 } } }
      }
      const keys = columns.slice(1).filter(k => labels[k])
      return {
        color: [color, '#8cc8ff'],
        tooltip: { trigger: 'axis' },
        legend: { data: keys.map(k => labels[k]), top: 0, right: 0 },
        grid: { left: 40, right: 16, top: 32, bottom: 28 },
        xAxis: { type: 'category', boundaryGap: false, data: rows.map(r => r[columns[0]]), axisLine: { lineStyle: { color: '#e5e6eb' } }, axisLabel: { color: '#86909c' } },
        yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f1f3' } }, axisLabel: { color: '#86909c' } },
        series: keys.map(k => ({
          name: labels[k],
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 5,
          areaStyle: { opacity: 0.08 },
          data: rows.map(r => r[k])
        }))
      }
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.dash {
  padding: 20px;
  background: var(--bg, #f7f8fa);
  min-height: calc(100vh - 84px);
}
.dash-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.dash-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 2px rgba(31, 35, 41, 0.04);
}
.dash-card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex: none;
}
.dash-card-label {
  font-size: 13px;
  color: var(--text-3, #86909c);
  margin-bottom: 6px;
}
.dash-card-num {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-1, #1f2329);
  font-variant-numeric: tabular-nums;
  line-height: 1;
}
.dash-charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 16px;
}
.dash-chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px 12px;
  box-shadow: 0 1px 2px rgba(31, 35, 41, 0.04);
}
.dash-chart-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1, #1f2329);
  margin-bottom: 8px;
}
.dash-chart {
  width: 100%;
  height: 300px;
}
.dash-bottom {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 16px;
  margin-top: 16px;
}
.dash-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.dash-panel {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 1px 2px rgba(31, 35, 41, 0.04);
}
.dash-panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1, #1f2329);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.dash-more {
  font-weight: 400;
}
.dash-todo-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 4px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s ease-out;

  &:hover {
    background: var(--bg, #f7f8fa);
  }
  & + .dash-todo-row {
    border-top: 1px solid var(--line, #e5e6eb);
  }
}
.dash-todo-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex: none;
}
.dash-todo-label {
  flex: 1;
  font-size: 14px;
  color: var(--text-2, #4e5969);
}
.dash-todo-num {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-1, #1f2329);
  font-variant-numeric: tabular-nums;
}
.dash-todo-arrow {
  color: var(--text-3, #86909c);
}
.dash-ai {
  cursor: pointer;
  transition: box-shadow 0.15s ease-out;

  &:hover {
    box-shadow: 0 6px 16px rgba(31, 35, 41, 0.08);
  }
}
.dash-ai-body {
  display: flex;
  align-items: center;
  gap: 32px;
}
.dash-ai-num {
  font-size: 22px;
  font-weight: 600;
  color: #1890ff;
  font-variant-numeric: tabular-nums;
}
.dash-ai-label {
  font-size: 12px;
  color: var(--text-3, #86909c);
  margin-top: 4px;
}
.dash-ai-body .dash-todo-arrow {
  margin-left: auto;
}
.dash-price {
  color: #d93a3a;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
@media (max-width: 1200px) {
  .dash-cards { grid-template-columns: repeat(2, 1fr); }
  .dash-charts { grid-template-columns: 1fr; }
  .dash-bottom { grid-template-columns: 1fr; }
}
@media (prefers-reduced-motion: reduce) {
  .dash-todo-row, .dash-ai { transition: none; }
}
</style>