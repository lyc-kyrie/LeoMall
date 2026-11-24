<template>
  <div class="sk">
    <div v-if="banner" class="sk_banner sk_anim"></div>
    <div v-if="cols > 0" class="sk_grid" :style="{ gridTemplateColumns: `repeat(${cols}, 1fr)` }">
      <div v-for="n in cols * 2" :key="n" class="sk_tile sk_anim"></div>
    </div>
    <div v-for="r in rows" :key="'r' + r" class="sk_row">
      <div v-if="thumb" class="sk_thumb sk_anim"></div>
      <div class="sk_lines">
        <div class="sk_line sk_anim" :style="{ width: r % 2 ? '82%' : '64%' }"></div>
        <div class="sk_line sk_anim sk_line_short"></div>
      </div>
    </div>
  </div>
</template>

<script>
// 阶段2D-2：骨架屏基元。banner=16:9 通栏块；cols=网格_tile 列数（渲染两行）；rows=文本行数；thumb=行首方图
export default {
  name: 'SkeletonBlock',
  props: {
    banner: { type: Boolean, default: false },
    cols: { type: Number, default: 0 },
    rows: { type: Number, default: 0 },
    thumb: { type: Boolean, default: false }
  }
};
</script>

<style lang="scss" scoped>
.sk {
  padding: 12px 16px;
}

.sk_anim {
  background: linear-gradient(90deg, #eceef1 25%, #f5f6f8 37%, #eceef1 63%);
  background-size: 400% 100%;
  animation: sk_shimmer 1.4s ease infinite;
}

@keyframes sk_shimmer {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}

@media (prefers-reduced-motion: reduce) {
  .sk_anim { animation: none; }
}

.sk_banner {
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 12px;
  margin-bottom: 12px;
}

.sk_grid {
  display: grid;
  gap: 12px;
  margin-bottom: 12px;
}

.sk_tile {
  aspect-ratio: 1 / 1;
  border-radius: 12px;
}

.sk_row {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 0;
}

.sk_thumb {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  flex-shrink: 0;
}

.sk_lines {
  flex: 1;
}

.sk_line {
  height: 14px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.sk_line_short {
  width: 40%;
  margin-bottom: 0;
}
</style>