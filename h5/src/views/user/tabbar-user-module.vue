<template>
  <div class="user_module">
    <div class="um_card">
      <van-cell title="我的收藏" to="/user/collect" is-link>
        <template #icon>
          <svg class="um_icon" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 4l2.35 4.76 5.25.76-3.8 3.7.9 5.24L12 16l-4.7 2.46.9-5.24-3.8-3.7 5.25-.76z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
          </svg>
        </template>
      </van-cell>
      <van-cell title="收货地址" to="/user/address" is-link>
        <template #icon>
          <svg class="um_icon" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2.5c-3.6 0-6.5 2.9-6.5 6.5 0 4.6 5.4 11.3 6.1 12.1a.55.55 0 0 0 .8 0c.7-.8 6.1-7.5 6.1-12.1 0-3.6-2.9-6.5-6.5-6.5z" fill="none" stroke="currentColor" stroke-width="1.8"/>
            <circle cx="12" cy="9" r="2.4" fill="none" stroke="currentColor" stroke-width="1.8"/>
          </svg>
        </template>
      </van-cell>
      <van-cell title="服务中心" to="/user/server" is-link>
        <template #icon>
          <svg class="um_icon" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M4 13a8 8 0 0 1 16 0" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <rect x="3" y="13" width="4" height="6" rx="2" fill="none" stroke="currentColor" stroke-width="1.8"/>
            <rect x="17" y="13" width="4" height="6" rx="2" fill="none" stroke="currentColor" stroke-width="1.8"/>
          </svg>
        </template>
      </van-cell>
    </div>

    <!-- AI 导购入口：品牌浅底卡，点击唤起全局 AgentChat -->
    <div class="ai_entry" @click="openAgent">
      <svg class="ai_entry_icon" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <path d="M12 3C6.6 3 2 6.7 2 11.2c0 2.3 1.2 4.3 3.1 5.7L4.4 21l4.2-1.9c1.1.3 2.2.5 3.4.5 5.4 0 10-3.7 10-8.4S17.4 3 12 3z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
        <circle cx="8" cy="11.2" r="1.1" fill="currentColor"/>
        <circle cx="12" cy="11.2" r="1.1" fill="currentColor"/>
        <circle cx="16" cy="11.2" r="1.1" fill="currentColor"/>
      </svg>
      <div class="ai_entry_body">
        <div class="ai_entry_title">AI 导购</div>
        <div class="ai_entry_desc">说出预算和喜好，帮你挑好物</div>
      </div>
      <svg class="ai_entry_arrow" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <path d="M9 5l7 7-7 7" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </div>
  </div>
</template>

<script>
// 阶段2D-3：service 圆角卡 + 品牌红 SVG 图标 + AI 导购浅底入口
import { Cell } from 'vant';

export default {
  name: 'user-module',
  methods: {
    openAgent() {
      window.dispatchEvent(new CustomEvent('leomall:agent-open'));
    }
  },
  components: {
    [Cell.name]: Cell
  }
};
</script>

<style scoped lang="scss">
.um_card {
  margin: 12px;
  background: var(--card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-1);
  overflow: hidden;
}
.um_icon {
  width: 18px;
  height: 18px;
  margin-right: 8px;
  color: var(--brand);
}

.ai_entry {
  margin: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: var(--brand-soft);
  border-radius: var(--radius-card);
  cursor: pointer;
  &:active {
    opacity: 0.85;
  }
}
.ai_entry_icon {
  flex: 0 0 auto;
  width: 26px;
  height: 26px;
  color: var(--brand);
}
.ai_entry_body {
  flex: 1 1 auto;
  min-width: 0;
}
.ai_entry_title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}
.ai_entry_desc {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-2);
}
.ai_entry_arrow {
  flex: 0 0 auto;
  width: 16px;
  height: 16px;
  color: var(--text-3);
}
</style>