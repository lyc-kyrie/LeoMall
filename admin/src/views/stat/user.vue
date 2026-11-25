<template>
  <div class="app-container">
    <div ref="chart" class="stat-chart" />
  </div>
</template>

<script>
// 阶段2D-4a：v-charts(Vue2) → echarts5 直连
import * as echarts from 'echarts'
import { statUser } from '@/api/stat'

export default {
  name: 'StatUser',
  data() {
    return { chart: null }
  },
  mounted() {
    this.chart = echarts.init(this.$refs.chart)
    this.onResize = () => this.chart.resize()
    window.addEventListener('resize', this.onResize)
    statUser().then(response => {
      const { columns, rows } = response.data.data
      this.chart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 48, right: 24, top: 32, bottom: 32 },
        xAxis: { type: 'category', boundaryGap: true, data: rows.map(r => r[columns[0]]) },
        yAxis: { type: 'value' },
        series: [{
          name: '用户增长数',
          type: 'bar',
          barMaxWidth: 28,
          itemStyle: { color: '#1890ff', borderRadius: [6, 6, 0, 0] },
          label: { show: true, position: 'top' },
          data: rows.map(r => r[columns[1]])
        }]
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