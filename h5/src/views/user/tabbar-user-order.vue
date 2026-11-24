<template>
  <div class="ug_card">
    <van-cell title="我的订单" value="全部订单" is-link to="/user/order/list/0" />
    <div class="order_status">
      <div class="order_status_item" @click="$router.push({ path: '/user/order/list/1' })">
        <div class="order_status_icon">
          <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <rect x="3" y="6" width="18" height="13" rx="2.5" fill="none" stroke="currentColor" stroke-width="1.8"/>
            <path d="M3 9.5h18" fill="none" stroke="currentColor" stroke-width="1.8"/>
            <circle cx="16.5" cy="14.5" r="1.3" fill="currentColor"/>
          </svg>
          <span class="order_badge" v-if="order.unpaid > 0">{{ order.unpaid }}</span>
        </div>
        <div class="order_status_label">待付款</div>
      </div>
      <div class="order_status_item" @click="$router.push({ path: '/user/order/list/2' })">
        <div class="order_status_icon">
          <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M3.5 7.5L12 3l8.5 4.5v9L12 21l-8.5-4.5z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M3.5 7.5L12 12l8.5-4.5M12 12v9" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
          </svg>
          <span class="order_badge" v-if="order.unship > 0">{{ order.unship }}</span>
        </div>
        <div class="order_status_label">待发货</div>
      </div>
      <div class="order_status_item" @click="$router.push({ path: '/user/order/list/3' })">
        <div class="order_status_icon">
          <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M2.5 6.5h12v10h-12z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M14.5 9.5h4l3 3v4h-7z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <circle cx="6.5" cy="17.5" r="1.8" fill="none" stroke="currentColor" stroke-width="1.8"/>
            <circle cx="17.5" cy="17.5" r="1.8" fill="none" stroke="currentColor" stroke-width="1.8"/>
          </svg>
          <span class="order_badge" v-if="order.unrecv > 0">{{ order.unrecv }}</span>
        </div>
        <div class="order_status_label">待收货</div>
      </div>
      <div class="order_status_item" @click="$router.push({ path: '/user/order/list/4' })">
        <div class="order_status_icon">
          <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <circle cx="12" cy="12" r="8.5" fill="none" stroke="currentColor" stroke-width="1.8"/>
            <path d="M8.5 12.2l2.4 2.4 4.6-4.8" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="order_badge" v-if="order.uncomment > 0">{{ order.uncomment }}</span>
        </div>
        <div class="order_status_label">已完成</div>
      </div>
    </div>
  </div>
</template>

<script>
// 阶段2D-3：圆角卡 + 品牌红内联 SVG 四宫格（规避图标字体缺码位）+ 红角标
import { Cell } from 'vant';
import { userIndex } from '@/api/api';

export default {
  name: 'order-group',

  data() {
    return {
      order: []
    };
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      // 匿名不请求：避免 501 全局 toast + 跳登录
      if (!localStorage.getItem('Authorization')) return;
      userIndex().then(res => {
        this.order = res.data.data.order;
      });
    }
  },
  components: {
    [Cell.name]: Cell
  }
};
</script>

<style scoped lang="scss">
.ug_card {
  margin: 12px;
  background: var(--card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-1);
  overflow: hidden;
}
.order_status {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  text-align: center;
  padding: 12px 0 14px;
}
.order_status_item {
  cursor: pointer;
}
.order_status_icon {
  position: relative;
  width: 26px;
  height: 26px;
  margin: 0 auto;
  color: var(--brand);
  svg {
    width: 100%;
    height: 100%;
    display: block;
  }
}
.order_badge {
  position: absolute;
  top: -6px;
  right: -10px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--brand);
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  font-variant-numeric: tabular-nums;
}
.order_status_label {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-2);
}
</style>