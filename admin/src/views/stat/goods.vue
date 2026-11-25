<template>
  <div class="app-container">
    <div ref="chart" class="stat-chart" />
  </div>
</template>

<script>
// 阶段2D-4a：v-charts(Vue2) → echarts5 直连
import * as echarts from 'echarts'
import { statGoods } from '@/api/stat'

const LABELS = { orders: '订单量', products: '下单货品数量', amount: '下单货品总额' }
const COLORS = ['#1890ff', '#22a06b', '#f5a623']

export default {
  name: 'StatGoods',
  data() {
    return { chart: null }
  },
  mounted() {
    this.chart = echarts.init(this.$refs.chart)
    this.onResize = () => this.chart.resize()
    window.addEventListener('resize', this.onResize)
    statGoods().then(response => {
      const { columns, rows } = response.data.data
      const keys = columns.slice(1)
      this.chart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: keys.map(k => LABELS[k] || k) },
        grid: { left: 56, right: 56, top: 40, bottom: 32 },
        xAxis: { type: 'category', boundaryGap: true, data: rows.map(r => r[columns[0]]) },
        yAxis: [
          { type: 'value', name: '单/件' },
          { type: 'value', name: '元' }
        ],
        series: keys.map((k, i) => ({
          name: LABELS[k] || k,
          type: 'line',
          smooth: true,
          yAxisIndex: k === 'amount' ? 1 : 0,
          itemStyle: { color: COLORS[i % COLORS.length] },
          data: rows.map(r => r[k])
        }))
      })
    })
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.onResize)
    this.chart.dispose()
  }
}
</script>

<style scoped>
.stat-chart {
  width: 100%;
  height: 420px;
  background: #fff;
  border-radius: 12px;
}
</style>