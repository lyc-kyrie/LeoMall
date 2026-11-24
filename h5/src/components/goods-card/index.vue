<template>
  <div class="gcard" @click="$emit('click', goods.id)">
    <div class="gcard_img">
      <img v-lazy="goods.picUrl" :alt="goods.name" @error="onImgError">
    </div>
    <div class="gcard_body">
      <div class="gcard_title">{{ goods.name }}</div>
      <div class="gcard_tags" v-if="goods.isNew || goods.isHot">
        <span v-if="goods.isNew" class="gcard_tag">新品</span>
        <span v-if="goods.isHot" class="gcard_tag gcard_tag_hot">热卖</span>
      </div>
      <div class="gcard_price">
        <span class="gcard_now">¥{{ goods.retailPrice }}</span>
        <span v-if="goods.counterPrice > goods.retailPrice" class="gcard_origin">¥{{ goods.counterPrice }}</span>
      </div>
    </div>
  </div>
</template>

<script>
// 阶段2D-2：共用商品卡（1:1 图 + 两行题 + 标签 + 红价），搜索/列表复用
import goodsDefaultImg from '@/assets/images/goods_default.png';

export default {
  name: 'GoodsCard',
  props: {
    goods: { type: Object, required: true }
  },
  emits: ['click'],
  methods: {
    onImgError(e) {
      const el = e.target;
      if (el.dataset.fallbacked) return;
      el.dataset.fallbacked = '1';
      el.src = goodsDefaultImg;
    }
  }
};
</script>

<style lang="scss" scoped>
.gcard {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(31, 35, 41, 0.04);
}

.gcard_img {
  aspect-ratio: 1 / 1;
  background: var(--bg);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.gcard_body {
  padding: 8px 10px 10px;
}

.gcard_title {
  font-size: 13px;
  color: var(--text-1);
  line-height: 1.4;
  height: 36px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.gcard_tags {
  margin-top: 4px;
  display: flex;
  gap: 4px;
}

.gcard_tag {
  font-size: 10px;
  color: var(--brand);
  background: var(--brand-soft);
  border-radius: 4px;
  padding: 1px 5px;
}

.gcard_tag_hot {
  color: var(--text-2);
  background: var(--bg);
}

.gcard_price {
  margin-top: 6px;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.gcard_now {
  color: var(--brand);
  font-size: 16px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.gcard_origin {
  color: var(--text-3);
  font-size: 11px;
  text-decoration: line-through;
}
</style>