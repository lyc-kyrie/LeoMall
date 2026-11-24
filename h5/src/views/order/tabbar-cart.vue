<template>
  <div class="tab-cart">
    <div class="cart_head">
      <span class="cart_head_title">购物车<em v-if="goods.length">({{ goods.length }})</em></span>
      <button class="cart_head_edit" type="button" v-show="goods.length" @click="isEditor = !isEditor">
        {{ isEditor ? '完成' : '编辑' }}
      </button>
    </div>

    <skeleton-block v-if="state === 'loading'" :rows="3" thumb />
    <error-state v-else-if="state === 'error'" :message="errMsg" @retry="init" />
    <empty-state
      v-else-if="!goods.length"
      icon="goods-collect-o"
      text="购物车空空如也"
      actionText="去逛逛"
      @action="$router.push('/')"
    />

    <van-checkbox-group v-else @change="toggle" class="card-goods" v-model="checkedGoods">
      <van-swipe-cell v-for="(item, i) in goods" :key="item.id">
        <div class="cart_row">
          <van-checkbox :name="item.id" v-model="item.checked" />
          <img class="cart_row_img" :src="item.picUrl" alt="">
          <div class="cart_row_info">
            <div class="cart_row_name">{{ item.goodsName }}</div>
            <div class="cart_row_specs" v-if="item.specifications && item.specifications.length">
              <span v-for="(spec, index) in item.specifications" :key="index">{{ spec }}</span>
            </div>
            <div class="cart_row_foot">
              <span class="cart_row_price">¥{{ item.price }}</span>
              <van-stepper v-if="isEditor" v-model="item.number" disable-input @change="val => stepperEvent(item, val)" />
              <span v-else class="cart_row_num">×{{ item.number }}</span>
            </div>
          </div>
        </div>
        <template #right>
          <div class="cart_swipe_del" @click="deleteCart(i)">删除</div>
        </template>
      </van-swipe-cell>
    </van-checkbox-group>

    <div class="cart_bar" v-if="state === 'ready' && goods.length">
      <van-checkbox v-model="checkedAll" class="cart_bar_all" @click="setCheckAll">全选</van-checkbox>
      <div class="cart_bar_total">
        <span>合计</span>
        <span class="cart_bar_price">¥{{ (totalPrice / 100).toFixed(2) }}</span>
      </div>
      <button
        class="cart_bar_btn"
        type="button"
        :disabled="!checkedGoods.length || isSubmit"
        @click="cartSubmit"
      >{{ submitBarText }}<em v-if="checkedGoods.length">({{ checkedGoods.length }})</em></button>
    </div>
  </div>
</template>

<script>
// 阶段2D-3：白卡行 + 品牌红 swipe 删除 + 壳内结算条 + 三态
import { Checkbox, CheckboxGroup, Stepper, SwipeCell, showConfirmDialog } from 'vant';
import { cartList, cartUpdate, cartChecked, cartDelete } from '@/api/api';
import { setLocalStorage } from '@/utils/local-storage';
import _ from 'lodash';

export default {
  name: 'tabbar-cart',
  data() {
    return {
      state: 'loading',
      errMsg: '',
      isEditor: false,
      checkedAll: false,
      isSubmit: false,
      checkedGoods: [],
      allGoods: [],
      goods: []
    };
  },
  created() {
    this.init();
  },
  computed: {
    submitBarText() {
      return this.isEditor ? '删除' : '结算';
    },
    totalPrice() {
      return this.goods.reduce(
        (total, item) =>
          total +
          (this.checkedGoods.indexOf(item.id) !== -1
            ? item.price * item.number * 100
            : 0),
        0
      );
    }
  },

  methods: {
    stepperEvent(item, number) {
      cartUpdate({
        number: number,
        goodsId: item.goodsId,
        id: item.id,
        productId: item.productId
      });
    },
    init() {
      this.state = 'loading';
      cartList(null, { silentError: true }).then(res => {
        this.goods = res.data.data.cartList;
        this.allGoods = this.getAllList();
        this.checkedGoods = this.getCheckedList(this.goods);
        this.checkedAll = this.goods.length > 0 && this.checkedGoods.length === this.allGoods.length;
        this.state = 'ready';
      }).catch(e => {
        this.errMsg = (e && e.data && e.data.errmsg) || '购物车加载失败';
        this.state = 'error';
      });
    },
    getAllList() {
      let result = [];
      _.each(this.goods, v => {
        result.push(v.id);
      });
      return result;
    },
    getCheckedList(goods) {
      let result = [];
      _.each(goods, v => {
        if (v.checked) {
          result.push(v.id);
        }
      });
      return result;
    },
    cartSubmit() {
      let productIds = [];
      let checkedGoods = this.checkedGoods;
      _.each(checkedGoods, id => {
        productIds.push(
          _.find(this.goods, vv => {
            return id === vv.id;
          }).productId
        );
      });
      if (this.isEditor) {
        showConfirmDialog({
          message: '确定删除所选商品吗?',
          cancelButtonText: '再想想'
        }).then(() => {
          this.deleteNext(productIds);
        });
      } else {
        this.isSubmit = true;
        setLocalStorage({ AddressId: 0, CartId: 0, CouponId: 0 });
        this.$router.push('/order/checkout');
      }
    },
    setCheckAll() {
      if (this.checkedGoods.length === this.allGoods.length) {
        this.checkedGoods = [];
      } else {
        this.checkedGoods = this.allGoods;
      }
    },
    deleteCart(o) {
      let productId = this.goods[o].productId;
      showConfirmDialog({ message: '确定删除所选商品吗', cancelButtonText: '再想想' })
        .then(() => {
          this.$nextTick(() => {
            this.deleteNext(productId);
          });
        });
    },
    toggle(index) {
      let addProductIds = [];
      _.each(index, v => {
        let productId = _.find(this.goods, result => {
          return result.id === v;
        }).productId;
        addProductIds.push(productId);
      });

      let delProductIds = [];
      _.each(_.difference(this.allGoods, index), v => {
        let productId = _.find(this.goods, result => {
          return result.id === v;
        }).productId;
        delProductIds.push(productId);
      });
      if (delProductIds.length > 0) {
        cartChecked({ productIds: delProductIds, isChecked: 0 });
      }
      if (addProductIds.length > 0) {
        cartChecked({ productIds: addProductIds, isChecked: 1 });
      }

      this.checkedAll = index.length === this.allGoods.length;
    },
    deleteNext(o) {
      let productIds = [];
      if (o instanceof Array) {
        productIds = o;
      } else {
        productIds.push(o);
      }

      cartDelete({ productIds: productIds }).then(res => {
        this.goods = res.data.data.cartList;
        this.allGoods = this.getAllList();
        this.checkedGoods = this.getCheckedList(this.goods);
        this.checkedAll = this.goods.length > 0 && this.checkedGoods.length === this.allGoods.length;
      });
    }
  },

  components: {
    [Stepper.name]: Stepper,
    [SwipeCell.name]: SwipeCell,
    [Checkbox.name]: Checkbox,
    [CheckboxGroup.name]: CheckboxGroup
  }
};
</script>

<style lang="scss" scoped>
.tab-cart {
  padding-bottom: 110px;
}

.cart_head {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: var(--card);
}
.cart_head_title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-1);
  em {
    font-style: normal;
    font-size: 12px;
    color: var(--text-3);
    margin-left: 4px;
  }
}
.cart_head_edit {
  border: none;
  background: transparent;
  color: var(--brand);
  font-size: 13px;
  padding: 4px 0;
}

.card-goods {
  padding: 12px 12px 0;
}
.cart_row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  margin-bottom: 12px;
  background: var(--card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-1);
}
.cart_row_img {
  flex: 0 0 auto;
  width: 72px;
  height: 72px;
  border-radius: var(--radius-img);
  object-fit: cover;
  background: var(--bg);
}
.cart_row_info {
  flex: 1 1 auto;
  min-width: 0;
}
.cart_row_name {
  font-size: 13px;
  color: var(--text-1);
  line-height: 1.4;
  height: 36px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.cart_row_specs {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  span {
    font-size: 10px;
    color: var(--text-3);
    background: var(--bg);
    border-radius: 4px;
    padding: 1px 5px;
  }
}
.cart_row_foot {
  margin-top: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cart_row_price {
  color: var(--brand);
  font-size: 16px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.cart_row_num {
  font-size: 12px;
  color: var(--text-3);
}
.cart_swipe_del {
  width: 64px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand);
  color: #fff;
  font-size: 14px;
}

/* 壳内固定结算条（与 2D-1 底栏定位同模式） */
.cart_bar {
  position: fixed;
  bottom: 50px;
  left: 50%;
  margin-left: calc(min(100%, var(--shell)) / -2);
  width: min(100%, var(--shell));
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: var(--card);
  box-shadow: 0 -1px 0 var(--line);
}
.cart_bar_all {
  flex: 0 0 auto;
  font-size: 13px;
  color: var(--text-2);
}
.cart_bar_total {
  flex: 1 1 auto;
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 6px;
  font-size: 12px;
  color: var(--text-2);
}
.cart_bar_price {
  color: var(--brand);
  font-size: 18px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.cart_bar_btn {
  flex: 0 0 auto;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--brand);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  height: 40px;
  padding: 0 22px;
  em {
    font-style: normal;
    font-size: 12px;
    margin-left: 2px;
  }
  &:disabled {
    opacity: 0.5;
  }
}
</style>