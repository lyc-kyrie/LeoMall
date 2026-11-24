<template>
  <div class="item_detail">
    <!-- 阶段2D 前置 quick wins：加载守卫，杜绝加载期 ¥NaN 裸奔 -->
    <div v-if="!goods.info.id" class="detail-loading">
      <van-loading type="spinner" color="#D93A3A" />
    </div>
    <template v-if="goods.info.id">
    <van-swipe :autoplay="3000">
      <van-swipe-item v-for="(image, index) in goods.info.gallery" :key="index">
        <img v-lazy="image" width="100%">
      </van-swipe-item>
    </van-swipe>
    <van-cell-group class="item_cell_group" v-if="goods">
      <van-cell class="item_info">
        <div>
          <span class="item_price">{{ yuan(goods.info.retailPrice*100) }}</span>
          <span class="item_market_price">{{ yuan(goods.info.counterPrice*100) }}</span>
        </div>
        <div class="item-title">
          {{ goods.info.name }}
        </div>
        <div class="item_intro">{{goods.info.brief}}</div>
      </van-cell>
    </van-cell-group>

  <div class="item_cell_group">
    <van-cell-group>
      <van-cell
        title="规格"
        isLink
        value="请选择"
        @click="skuClick"
      />
      <van-cell title="属性" isLink @click="propsPopup = true"/>
      <van-cell title="运费" value="满88免邮费"/>
    </van-cell-group>
    <van-popup v-model:show="showSku" position="bottom" round class="sku_popup">
      <div class="sku_head">
        <img :src="goods.info.picUrl" alt="">
        <div class="sku_head_info">
          <div class="sku_price">¥{{ skuPrice }}</div>
          <div class="sku_selected">{{ selectedText || '请选择规格' }}</div>
        </div>
        <van-icon name="cross" class="sku_close" @click="showSku = false"/>
      </div>
      <div class="sku_body">
        <div class="sku_group" v-for="(spec, si) in goods.specificationList" :key="spec.id">
          <div class="sku_group_name">{{spec.name}}</div>
          <div class="sku_values">
            <span class="sku_value"
                  :class="{active: selected[si] === vv.id}"
                  v-for="vv in spec.valueList"
                  :key="vv.id"
                  @click="pick(si, vv.id)">{{vv.value}}</span>
          </div>
        </div>
        <div class="sku_group">
          <div class="sku_group_name">数量</div>
          <van-stepper v-model="selectedNum" min="1" max="999"/>
        </div>
      </div>
      <div class="sku_foot">
        <van-button round class="sku_btn_cart" @click="submitSku('cart')">加入购物车</van-button>
        <van-button round type="danger" class="sku_btn_buy" @click="submitSku('buy')">立即购买</van-button>
      </div>
    </van-popup>
    <van-popup v-model:show="propsPopup" position="bottom">
      <popup-props :propsStr="props_str" @close="propsPopup = false"></popup-props>
    </van-popup>
  </div>

    <div class="item_desc">
      <div class="item_desc_title">商品详情</div>
      <div class="item_desc_wrap" v-if="goods.info.detail" v-html="goods.info.detail"></div>
      <div class="item_desc_wrap" v-else style="text-align: center;">
        <p>无详情</p>
      </div>
    </div>

    <van-action-bar>
      <van-action-bar-icon @click="toCart" icon="cart-o" :badge="(cartInfo > 0) ? cartInfo : ''"/>
      <van-action-bar-icon @click="addCollect" icon="star-o" :style="(goods.userHasCollect !== 0) ? 'color: #f7b444;':''"/>
      <van-action-bar-button type="warning" @click="skuClick" text="加入购物车"/>
      <van-action-bar-button type="danger" @click="skuClick" text="立即购买"/>
    </van-action-bar>
    </template>

  </div>
</template>

<script>

import { goodsDetail, cartGoodsCount, collectAddOrDelete, cartAdd, cartFastAdd } from '@/api/api';

import { Swipe, SwipeItem, ActionBar, ActionBarButton, ActionBarIcon, Popup, showToast } from 'vant';
import { setLocalStorage } from '@/utils/local-storage';
import popupProps from './popup-props';
import _ from 'lodash';

export default {
  props: {
    itemId: [String, Number]
  },

  data() {
    const isLogin = !!localStorage.getItem('Authorization');

    return {
      isLogin,
      goods: {
        userHasCollect: 0,
        info: {
          gallery: []
        }
      },
      cartInfo: 0,
      selected: [],
      selectedNum: 1,
      propsPopup: false,
      showSku: false
    };
  },

  computed: {
    props_str() {
      let props_arr = [];
      _.each(this.goods.attribute, json => {
        props_arr.push([json['attribute'], json['value']]);
      });
      return props_arr || [];
    },
    currentProductId() {
      if (this.selected[0] && this.selected[1]) {
        return this.getProductId(this.selected[0], this.selected[1]);
      }
      if (this.selected[0]) {
        return this.getProductIdByOne(this.selected[0]);
      }
      return 0;
    },
    skuPrice() {
      const product = _.find(this.goods.productList, v => v.id === this.currentProductId);
      return product ? product.price : this.goods.info.retailPrice;
    },
    selectedText() {
      const names = [];
      _.each(this.goods.specificationList, (spec, si) => {
        const vv = _.find(spec.valueList, v => v.id === this.selected[si]);
        if (vv) names.push(vv.value);
      });
      return names.join(' / ');
    }
  },

  // 路由参数变化（同组件复用）时重新加载，避免展示上一个商品的数据
  watch: {
    itemId() {
      this.goods = { userHasCollect: 0, info: { gallery: [] } };
      this.initData();
    }
  },

  created() {
    this.initData();
  },

  methods: {
    skuClick() {
      this.showSku = true;
    },
    initData() {
      goodsDetail({ id: this.itemId }).then(res => {
        this.goods = res.data.data;
        this.selected = [];
        this.selectedNum = 1;
      });

      cartGoodsCount().then(res => {
        this.cartInfo = res.data.data;
      });
    },
    toCart() {
      this.$router.push({
        name: 'cart'
      });
    },
    addCollect() {
      collectAddOrDelete({ valueId: this.itemId, type: 0 }).then(res => {
        if (this.goods.userHasCollect === 1) {
          this.goods.userHasCollect = 0;
        } else {
          this.goods.userHasCollect = 1;
          showToast({
            message: '收藏成功',
            duration: 1500
          });
        }
      });
    },
    getProductId(s1, s2) {
      var productId;
      var s1_name;
      var s2_name;
      _.each(this.goods.specificationList, specification => {
        _.each(specification.valueList, specValue => {
          if (specValue.id === s1) {
            s1_name = specValue.value;
          } else if (specValue.id === s2) {
            s2_name = specValue.value;
          }
        });
      });

      _.each(this.goods.productList, v => {
        let result = _.without(v.specifications, s1_name, s2_name);
        if (result.length === 0) {
          productId = v.id;
        }
      });
      return productId;
    },
    getProductIdByOne(s1) {
      var productId;
      var s1_name;
      _.each(this.goods.specificationList, specification => {
        _.each(specification.valueList, specValue => {
          if (specValue.id === s1) {
            s1_name = specValue.value;
            return;
          }
        });
      });

      _.each(this.goods.productList, v => {
        let result = _.without(v.specifications, s1_name);
        if (result.length === 0) {
          productId = v.id;
        }
      });
      return productId;
    },
    addCart(data) {
      let that = this;
      let params = {
        goodsId: data.goodsId,
        number: data.selectedNum,
        productId: 0
      };
      if (_.has(data.selectedSkuComb, 's3')) {
        showToast({
          message: '目前仅支持两规格',
          duration: 1500
        });
        return;
      } else if (_.has(data.selectedSkuComb, 's2')) {
        params.productId = this.getProductId(
          data.selectedSkuComb.s1,
          data.selectedSkuComb.s2
        );
      } else {
        params.productId = this.getProductIdByOne(data.selectedSkuComb.s1);
      }
      cartAdd(params).then(() => {
        this.cartInfo = this.cartInfo + data.selectedNum;
        showToast({
          message: '已添加至购物车',
          duration: 1500
        });
        that.showSku = false;
      });
    },
    buyGoods(data) {
      let that = this;
      let params = {
        goodsId: data.goodsId,
        number: data.selectedNum,
        productId: 0
      };
      if (_.has(data.selectedSkuComb, 's3')) {
        showToast({
          message: '目前仅支持两规格',
          duration: 1500
        });
        return;
      } else if (_.has(data.selectedSkuComb, 's2')) {
        params.productId = this.getProductId(
          data.selectedSkuComb.s1,
          data.selectedSkuComb.s2
        );
      } else {
        params.productId = this.getProductIdByOne(data.selectedSkuComb.s1);
      }
      cartFastAdd(params).then(res => {
        let cartId = res.data.data;
        setLocalStorage({ CartId: cartId });
        that.showSku = false;
        this.$router.push('/order/checkout');
      });
    },
    pick(si, id) {
      this.selected[si] = this.selected[si] === id ? undefined : id;
      this.selected = [...this.selected];
    },
    submitSku(mode) {
      const groups = this.goods.specificationList.length;
      const picked = this.selected.filter(v => v != null).length;
      if (picked < groups) {
        showToast({ message: '请选择规格', duration: 1500 });
        return;
      }
      if (groups > 0 && !this.currentProductId) {
        showToast({ message: '该规格组合暂无库存', duration: 1500 });
        return;
      }
      const comb = {};
      this.selected.forEach((v, i) => { if (v != null) comb['s' + (i + 1)] = v; });
      const data = {
        goodsId: this.goods.info.id,
        selectedNum: this.selectedNum,
        selectedSkuComb: comb
      };
      if (mode === 'cart') this.addCart(data);
      else this.buyGoods(data);
    }
  },

  components: {
    [Popup.name]: Popup,
    [Swipe.name]: Swipe,
    [SwipeItem.name]: SwipeItem,
    [ActionBar.name]: ActionBar,
    [ActionBarButton.name]: ActionBarButton,
    [ActionBarIcon.name]: ActionBarIcon,
    'popup-props': popupProps
  }
};
</script>

<style lang="scss" scoped>
.item_detail {
  img {
    max-width: 100%;
  }
}

.detail-loading {
  padding: 120px 0;
  text-align: center;
  background-color: #fff;
}

.item_cell_group {
  margin-bottom: 15px;
}

.item_price {
  font-size: 20px;
  color: $red;
  margin-right: 10px;
}

.item_market_price {
  color: $font-color-gray;
  text-decoration: line-through;
  font-size: $font-size-small;
}

.item-title {
  line-height: 1.4;
}

.item_dispatch {
  font-size: $font-size-small;
  color: $font-color-gray;
}

.item_intro {
  line-height: 18px;
  margin: 5px 0;
  font-size: $font-size-small;
  color: $font-color-gray;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.item_desc {
  background-color: #fff;
  :deep(p) {
    padding: 0 10px;
    margin-block-start: 0 !important;
    margin-block-end: 0 !important;
  }
  :deep(img) {
    max-width: 100%;
    display: block;
  }
}

.item_desc_title {
  @include one-border;
  padding: 10px 0;
  text-align: center;
}

.sku_head {
  display: flex;
  gap: 12px;
  padding: 16px;
  align-items: flex-start;

  img {
    width: 80px;
    height: 80px;
    object-fit: cover;
    border-radius: 8px;
    background: var(--bg);
  }
}

.sku_head_info {
  flex: 1;
}

.sku_price {
  color: var(--brand);
  font-size: 20px;
  font-weight: 700;
}

.sku_selected {
  color: var(--text-3);
  font-size: 12px;
  margin-top: 6px;
}

.sku_close {
  color: var(--text-3);
  font-size: 18px;
}

.sku_body {
  padding: 0 16px;
  max-height: 45vh;
  overflow-y: auto;
}

.sku_group {
  padding: 10px 0;
}

.sku_group_name {
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 8px;
}

.sku_values {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sku_value {
  padding: 6px 14px;
  border-radius: 999px;
  background: var(--bg);
  border: 1px solid transparent;
  font-size: 13px;
  color: var(--text-1);

  &.active {
    color: var(--brand);
    background: var(--brand-soft);
    border-color: var(--brand);
  }
}

.sku_foot {
  display: flex;
  gap: 8px;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));

  .van-button {
    flex: 1;
    height: 42px;
  }
}

.sku_btn_cart {
  color: var(--brand);
  border: 1px solid var(--brand);
}
</style>
