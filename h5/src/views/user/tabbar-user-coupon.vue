<template>
  <div class="ug_card">
    <van-cell title="我的优惠券" value="全部优惠券" is-link to="/user/coupon/list/0" />
    <div class="coupon_status">
      <div class="coupon_status_item" @click="$router.push({ path: '/user/coupon/list/0' })">
        <div class="coupon_status_icon">
          <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M3 9V7a1.5 1.5 0 0 1 1.5-1.5h15A1.5 1.5 0 0 1 21 7v2a2.5 2.5 0 0 0 0 5v2a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 16v-2a2.5 2.5 0 0 0 0-5z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M14.5 6v12" fill="none" stroke="currentColor" stroke-width="1.6" stroke-dasharray="2 2.4"/>
          </svg>
        </div>
        <div class="coupon_status_label">待使用</div>
      </div>
      <div class="coupon_status_item" @click="$router.push({ path: '/user/coupon/list/1' })">
        <div class="coupon_status_icon">
          <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M3 9V7a1.5 1.5 0 0 1 1.5-1.5h15A1.5 1.5 0 0 1 21 7v2a2.5 2.5 0 0 0 0 5v2a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 16v-2a2.5 2.5 0 0 0 0-5z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M6.5 11.5l2 2 3.5-3.5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="coupon_status_label">已使用</div>
      </div>
      <div class="coupon_status_item" @click="$router.push({ path: '/user/coupon/list/2' })">
        <div class="coupon_status_icon">
          <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M3 9V7a1.5 1.5 0 0 1 1.5-1.5h15A1.5 1.5 0 0 1 21 7v2a2.5 2.5 0 0 0 0 5v2a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 16v-2a2.5 2.5 0 0 0 0-5z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M7 9.5l4 4M11 9.5l-4 4" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="coupon_status_label">过期关闭</div>
      </div>
    </div>
  </div>
</template>

<script>
// 阶段2D-3：圆角卡 + 品牌红内联 SVG 三宫格
import { Cell } from 'vant';

export default {
  name: 'coupon-group',
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
.coupon_status {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  text-align: center;
  padding: 12px 0 14px;
}
.coupon_status_item {
  cursor: pointer;
}
.coupon_status_icon {
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
.coupon_status_label {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-2);
}
</style>