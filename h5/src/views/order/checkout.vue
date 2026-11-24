<template>
  <div class="checkout">
    <skeleton-block v-if="state === 'loading'" banner :rows="3" thumb />
    <error-state v-else-if="state === 'error'" :message="errMsg" @retry="init" />
    <empty-state
      v-else-if="!checkedGoodsList.length"
      icon="goods-collect-o"
      text="没有待结算的商品"
      actionText="去逛逛"
      @action="$router.push('/')"
    />

    <template v-else>
      <!-- 地址卡 -->
      <div class="ck_card ck_address" @click="goAddressList">
        <svg class="ck_pin" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 2.5c-3.6 0-6.5 2.9-6.5 6.5 0 4.6 5.4 11.3 6.1 12.1a.55.55 0 0 0 .8 0c.7-.8 6.1-7.5 6.1-12.1 0-3.6-2.9-6.5-6.5-6.5z" fill="none" stroke="currentColor" stroke-width="1.8"/>
          <circle cx="12" cy="9" r="2.4" fill="none" stroke="currentColor" stroke-width="1.8"/>
        </svg>
        <div class="ck_address_body" v-if="checkedAddress && checkedAddress.name">
          <div class="ck_address_line">
            <span class="ck_address_name">{{ checkedAddress.name }}</span>
            <span class="ck_address_tel">{{ checkedAddress.tel }}</span>
          </div>
          <div class="ck_address_detail">{{ checkedAddress.addressDetail }}</div>
        </div>
        <div class="ck_address_body ck_address_empty" v-else>添加收货地址</div>
        <svg class="ck_arrow" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path d="M9 5l7 7-7 7" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>

      <!-- 商品组卡 -->
      <div class="ck_card ck_goods">
        <div class="ck_goods_row" v-for="item in checkedGoodsList" :key="item.id">
          <img class="ck_goods_img" :src="item.picUrl" alt="">
          <div class="ck_goods_info">
            <div class="ck_goods_name">{{ item.goodsName }}</div>
            <div class="ck_goods_specs" v-if="item.specifications && item.specifications.length">
              <span v-for="(spec, index) in item.specifications" :key="index">{{ spec }}</span>
            </div>
          </div>
          <div class="ck_goods_price">
            <span class="ck_now">¥{{ item.price }}</span>
            <span class="ck_num">×{{ item.number }}</span>
          </div>
        </div>
      </div>

      <!-- 券 + 金额明细 + 备注 -->
      <div class="ck_card ck_summary">
        <van-cell class="ck_coupon" title="优惠券" is-link :value="getCouponValue()" @click="getCoupons" />
        <van-cell title="商品金额"><span class="ck_amount">¥{{ goodsTotalPrice.toFixed(2) }}</span></van-cell>
        <van-cell title="邮费"><span class="ck_amount">¥{{ freightPrice.toFixed(2) }}</span></van-cell>
        <van-cell title="优惠券"><span class="ck_amount ck_minus">-¥{{ couponPrice.toFixed(2) }}</span></van-cell>
        <van-field v-model="message" placeholder="请输入备注" label="订单备注" maxlength="50" show-word-limit />
      </div>

      <!-- 壳内提交条 -->
      <div class="ck_bar">
        <div class="ck_bar_total">
          <span>总计</span>
          <span class="ck_bar_price">¥{{ actualPrice.toFixed(2) }}</span>
        </div>
        <button class="ck_bar_btn" type="button" :disabled="isDisabled" @click="onSubmit">提交订单</button>
      </div>
    </template>

    <!-- 优惠券列表 -->
    <van-popup v-model:show="showList" position="bottom" round>
      <van-coupon-list
        :coupons="coupons"
        :chosen-coupon="chosenCoupon"
        :disabled-coupons="disabledCoupons"
        @change="onChange"
        @exchange="onExchange"
      />
    </van-popup>
  </div>
</template>

<script>
// 阶段2D-3：地址卡/商品组卡/金额明细卡 + 壳内红胶囊提交条 + 三态
import { Cell, CellGroup, Field, Popup, CouponList, showToast, showFailToast } from 'vant';
import { cartCheckout, orderSubmit, couponSelectList } from '@/api/api';
import { getLocalStorage, setLocalStorage } from '@/utils/local-storage';

export default {
  name: 'checkout-view',
  data() {
    return {
      state: 'loading',
      errMsg: '',
      checkedGoodsList: [],
      checkedAddress: {},
      availableCouponLength: 0,
      goodsTotalPrice: 0,
      freightPrice: 0,
      couponPrice: 0,
      grouponPrice: 0,
      orderTotalPrice: 0,
      actualPrice: 0,
      message: '',

      isDisabled: false,
      showList: false,
      chosenCoupon: -1,
      coupons: [],
      disabledCoupons: []
    };
  },
  created() {
    this.init();
  },

  methods: {
    onSubmit() {
      const { AddressId, CartId, CouponId, UserCouponId } = getLocalStorage('AddressId', 'CartId', 'CouponId', 'UserCouponId');

      if (AddressId === null || AddressId === '0') {
        showFailToast('请设置收货地址');
        return;
      }

      this.isDisabled = true;

      orderSubmit({
        addressId: AddressId,
        cartId: CartId,
        couponId: CouponId,
        userCouponId: UserCouponId,
        grouponLinkId: 0,
        grouponRulesId: 0,
        message: this.message
      }).then(res => {
        // 下单成功，重置下单参数。
        setLocalStorage({ AddressId: 0, CartId: 0, CouponId: 0 });

        let orderId = res.data.data.orderId;
        this.$router.push({
          name: 'payment',
          params: { orderId: orderId }
        });
      }).catch(() => {
        this.isDisabled = false;
        showToast('下单失败');
      });
    },
    goAddressList() {
      this.$router.push({ path: '/user/address' });
    },
    getCouponValue() {
      if (this.couponPrice !== 0) {
        return '-¥' + this.couponPrice.toFixed(2);
      }
      if (this.availableCouponLength !== 0) {
        return this.availableCouponLength + '张可用';
      }
      return '没有可用优惠券';
    },
    getCoupons() {
      const { CartId } = getLocalStorage('CartId');
      couponSelectList({ cartId: CartId, grouponRulesId: 0 }).then(res => {
        var cList = res.data.data.list;
        this.coupons = [];
        this.disabledCoupons = [];
        for (var i = 0; i < cList.length; i++) {
          var c = cList[i];
          var coupon = {
            id: c.id,
            cid: c.cid,
            name: c.name,
            condition: '满' + c.min + '元可用',
            value: c.discount * 100,
            description: c.desc,
            startAt: new Date(c.startTime).getTime() / 1000,
            endAt: new Date(c.endTime).getTime() / 1000,
            valueDesc: c.discount,
            unitDesc: '元'
          };
          if (c.available) {
            this.coupons.push(coupon);
          } else {
            this.disabledCoupons.push(coupon);
          }
        }
        this.showList = true;
      });
    },
    init() {
      this.state = 'loading';
      const { AddressId, CartId, CouponId, UserCouponId } = getLocalStorage('AddressId', 'CartId', 'CouponId', 'UserCouponId');

      cartCheckout({ cartId: CartId, addressId: AddressId, couponId: CouponId, userCouponId: UserCouponId, grouponRulesId: 0 }, { silentError: true }).then(res => {
        var data = res.data.data;

        this.checkedGoodsList = data.checkedGoodsList;
        this.checkedAddress = data.checkedAddress;
        this.availableCouponLength = data.availableCouponLength;
        this.actualPrice = data.actualPrice;
        this.couponPrice = data.couponPrice;
        this.grouponPrice = data.grouponPrice;
        this.freightPrice = data.freightPrice;
        this.goodsTotalPrice = data.goodsTotalPrice;
        this.orderTotalPrice = data.orderTotalPrice;

        setLocalStorage({ AddressId: data.addressId, CartId: data.cartId, CouponId: data.couponId, UserCouponId: data.userCouponId });
        this.state = 'ready';
      }).catch(e => {
        this.errMsg = (e && e.data && e.data.errmsg) || '结算信息加载失败';
        this.state = 'error';
      });
    },
    onChange(index) {
      this.showList = false;
      this.chosenCoupon = index;

      if (index === -1) {
        setLocalStorage({ CouponId: -1, UserCouponId: -1 });
      } else {
        const couponId = this.coupons[index].cid;
        const userCouponId = this.coupons[index].id;
        setLocalStorage({ CouponId: couponId, UserCouponId: userCouponId });
      }

      this.init();
    },
    onExchange() {
      showToast('兑换暂不支持');
    }
  },

  components: {
    [Cell.name]: Cell,
    [CellGroup.name]: CellGroup,
    [Field.name]: Field,
    [Popup.name]: Popup,
    [CouponList.name]: CouponList
  }
};
</script>

<style lang="scss" scoped>
.checkout {
  padding: 12px 12px 76px;
}

.ck_card {
  background: var(--card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-1);
  margin-bottom: 12px;
  overflow: hidden;
}

.ck_address {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 12px;
}
.ck_pin {
  flex: 0 0 auto;
  width: 22px;
  height: 22px;
  color: var(--brand);
}
.ck_address_body {
  flex: 1 1 auto;
  min-width: 0;
}
.ck_address_line {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.ck_address_name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-1);
}
.ck_address_tel {
  font-size: 13px;
  color: var(--text-2);
  font-variant-numeric: tabular-nums;
}
.ck_address_detail {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-2);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.ck_address_empty {
  font-size: 14px;
  font-weight: 600;
  color: var(--brand);
}
.ck_arrow {
  flex: 0 0 auto;
  width: 16px;
  height: 16px;
  color: var(--text-3);
}

.ck_goods {
  padding: 4px 12px;
}
.ck_goods_row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  & + .ck_goods_row {
    border-top: 1px solid var(--line);
  }
}
.ck_goods_img {
  flex: 0 0 auto;
  width: 64px;
  height: 64px;
  border-radius: var(--radius-img);
  object-fit: cover;
  background: var(--bg);
}
.ck_goods_info {
  flex: 1 1 auto;
  min-width: 0;
}
.ck_goods_name {
  font-size: 13px;
  color: var(--text-1);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.ck_goods_specs {
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
.ck_goods_price {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.ck_now {
  color: var(--brand);
  font-size: 14px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.ck_num {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-3);
}

.ck_summary {
  :deep(.van-cell) {
    background: transparent;
  }
}
.ck_coupon {
  :deep(.van-cell__value) {
    color: var(--brand);
  }
}
.ck_amount {
  color: var(--text-1);
  font-variant-numeric: tabular-nums;
}
.ck_minus {
  color: var(--brand);
}

/* 壳内固定提交条 */
.ck_bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  margin-left: calc(min(100%, var(--shell)) / -2);
  width: min(100%, var(--shell));
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 12px;
  padding-bottom: calc(8px + env(safe-area-inset-bottom));
  background: var(--card);
  box-shadow: 0 -1px 0 var(--line);
}
.ck_bar_total {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 12px;
  color: var(--text-2);
}
.ck_bar_price {
  color: var(--brand);
  font-size: 20px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.ck_bar_btn {
  border: none;
  border-radius: var(--radius-pill);
  background: var(--brand);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  height: 40px;
  padding: 0 26px;
  &:disabled {
    opacity: 0.5;
  }
}
</style>