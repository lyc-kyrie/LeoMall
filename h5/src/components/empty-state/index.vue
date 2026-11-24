<template>
  <div class="es">
    <div class="es_icon">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
        <template v-if="icon === 'apps-o'">
          <rect x="4" y="4" width="6.5" height="6.5" rx="1.5"/>
          <rect x="13.5" y="4" width="6.5" height="6.5" rx="1.5"/>
          <rect x="4" y="13.5" width="6.5" height="6.5" rx="1.5"/>
          <rect x="13.5" y="13.5" width="6.5" height="6.5" rx="1.5"/>
        </template>
        <template v-else-if="icon === 'goods-collect-o'">
          <path d="M5 8h14l-1.2 12H6.2L5 8z"/>
          <path d="M9 8V6a3 3 0 0 1 6 0v2"/>
        </template>
        <template v-else>
          <circle cx="10.5" cy="10.5" r="6"/>
          <line x1="15" y1="15" x2="20" y2="20"/>
        </template>
      </svg>
    </div>
    <div class="es_text">{{ text }}</div>
    <van-button v-if="actionText" round class="es_btn" @click="$emit('action')">{{ actionText }}</van-button>
  </div>
</template>

<script>
// 阶段2D-2：空态基元。icon 走 Vant 图标名，actionText 存在时渲染胶囊次 CTA 并 emit action
export default {
  name: 'EmptyState',
  props: {
    text: { type: String, default: '暂无内容' },
    icon: { type: String, default: 'search' },
    actionText: { type: String, default: '' }
  },
  emits: ['action']
};
</script>

<style lang="scss" scoped>
.es {
  padding: 72px 24px;
  text-align: center;
}

.es_icon {
  width: 88px;
  height: 88px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: var(--bg);
  display: flex;
  align-items: center;
  justify-content: center;

  svg {
    width: 36px;
    height: 36px;
    color: var(--text-3);
  }
}

.es_text {
  color: var(--text-3);
  font-size: 13px;
  margin-bottom: 16px;
}

.es_btn {
  height: 36px;
  padding: 0 24px;
  color: var(--brand);
  border: 1px solid var(--brand);
  background: #fff;
  font-size: 13px;
}
</style>