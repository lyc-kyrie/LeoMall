<template>
  <div class="ers">
    <div class="ers_icon">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
        <circle cx="12" cy="12" r="9"/>
        <line x1="12" y1="7.5" x2="12" y2="13"/>
        <circle cx="12" cy="16.2" r="0.6" fill="currentColor" stroke="none"/>
      </svg>
    </div>
    <div class="ers_text">{{ message }}</div>
    <van-button round class="ers_btn" @click="$emit('retry')">重试</van-button>
  </div>
</template>

<script>
// 阶段2D-2：错误态基元。重试 emit retry，由页面重发请求
export default {
  name: 'ErrorState',
  props: {
    message: { type: String, default: '加载失败，请检查网络后重试' }
  },
  emits: ['retry']
};
</script>

<style lang="scss" scoped>
.ers {
  padding: 72px 24px;
  text-align: center;
}

.ers_icon {
  width: 88px;
  height: 88px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: var(--brand-soft);
  display: flex;
  align-items: center;
  justify-content: center;

  svg {
    width: 36px;
    height: 36px;
    color: var(--brand);
  }
}

.ers_text {
  color: var(--text-2);
  font-size: 13px;
  margin-bottom: 16px;
}

.ers_btn {
  height: 36px;
  padding: 0 24px;
  color: #fff;
  background: var(--brand);
  border: none;
  font-size: 13px;
}
</style>