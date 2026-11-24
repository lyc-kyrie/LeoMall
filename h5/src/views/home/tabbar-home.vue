<template>
  <div class="tab_home">
    <div class="home_search">
      <van-search shape="round"
                  placeholder="点击前往搜索"
                  background="transparent"
                  @click="$router.push({ name: 'search' })" />
    </div>

    <div class="home_banner">
      <van-swipe :autoplay="3000"
                 indicator-color="#D93A3A">
        <van-swipe-item v-for="(banner, index) in shopInfos.banner"
                        :key="index">
          <img :src="banner.url" class="banner_img" alt="">
        </van-swipe-item>
      </van-swipe>
    </div>

    <div class="kingkong">
      <div class="kk_item"
           @click="changeTabbar(channel)"
           v-for="(channel, index) in shopInfos.channel"
           :key="index">
        <div class="kk_tile">
          <img :src="channel.iconUrl" alt="">
        </div>
        <span>{{channel.name}}</span>
      </div>
    </div>

    <div class="sec_head">
      <span class="sec_title">优惠券</span>
    </div>
    <div class="ticket_row">
      <div class="ticket"
           v-for="(coupon,index) in shopInfos.couponList"
           :key="index"
           @click="getCoupon(coupon.id)">
        <div class="ticket_amount"><span class="cur">¥</span>{{coupon.discount}}</div>
        <div class="ticket_body">
          <div class="ticket_name">{{coupon.name}}</div>
          <div class="ticket_desc">{{coupon.desc}} · {{coupon.tag}}</div>
          <div class="ticket_desc">有效期 {{coupon.days}} 天</div>
        </div>
        <div class="ticket_btn">领取</div>
      </div>
    </div>

    <div class="sec_head" v-if="shopInfos.grouponList && shopInfos.grouponList.length">
      <span class="sec_title">团购专区</span>
      <router-link to="/items/groupon" class="sec_more">更多团购商品</router-link>
    </div>
    <div class="hcard_list" v-if="shopInfos.grouponList && shopInfos.grouponList.length">
      <a class="hcard"
         v-for="(grouponGood ,index) in shopInfos.grouponList"
         :key="index"
         :href="goDetail(grouponGood.id)">
        <img :src="grouponGood.picUrl" alt="">
        <div class="hcard_body">
          <div class="hcard_title">{{grouponGood.name}}</div>
          <div class="hcard_tags">
            <van-tag plain type="primary">{{grouponGood.grouponMember}}人成团</van-tag>
            <van-tag plain type="danger">{{grouponGood.grouponDiscount}}元再减</van-tag>
          </div>
          <div class="price_row">
            <span class="price_now">¥{{grouponGood.grouponPrice}}</span>
            <span class="price_origin">¥{{grouponGood.retailPrice}}</span>
          </div>
        </div>
      </a>
    </div>

    <div class="sec_head">
      <span class="sec_title">品牌商直供</span>
      <router-link to="/items/brand-list" class="sec_more">更多品牌商</router-link>
    </div>
    <div class="hscroll_row">
      <a class="brand_card"
         v-for="(brand ,index) in shopInfos.brandList"
         :key="index"
         :href="goBrand(brand.id)">
        <img :src="brand.picUrl" alt="">
        <span>{{ brand.name }}</span>
      </a>
    </div>

    <div class="sec_head">
      <span class="sec_title">新品首发</span>
      <router-link to="/items/new" class="sec_more">更多新品首发</router-link>
    </div>
    <div class="gcard_grid">
      <router-link class="gcard"
                   v-for="(newGood ,index) in shopInfos.newGoodsList"
                   :key="index"
                   :to="{ path: `/items/detail/${newGood.id}`}">
        <img :src="newGood.picUrl" alt="">
        <div class="gcard_name">{{newGood.name}}</div>
        <div class="gcard_price">¥{{newGood.retailPrice}}</div>
      </router-link>
    </div>

    <div class="sec_head">
      <span class="sec_title">人气推荐</span>
      <router-link to="/items/hot" class="sec_more">更多人气推荐</router-link>
    </div>
    <div class="gcard_grid">
      <router-link class="gcard"
                   v-for="(groupGood ,index) in shopInfos.hotGoodsList"
                   :key="index"
                   :to="{ path: `/items/detail/${groupGood.id}`}">
        <img :src="groupGood.picUrl" alt="">
        <div class="gcard_name">{{groupGood.name}}</div>
        <div class="gcard_price">
          ¥{{groupGood.retailPrice}}
          <span class="price_origin">¥{{groupGood.counterPrice}}</span>
        </div>
      </router-link>
    </div>

    <div class="sec_head">
      <span class="sec_title">专题精选</span>
      <router-link to="/items/topic-list" class="sec_more">更多专题精选</router-link>
    </div>
    <div class="hscroll_row">
      <a class="topic_card"
         v-for="(topic ,index) in shopInfos.topicList"
         :key="index"
         :href="goTopic(topic.id)">
        <img :src="topic.picUrl" alt="">
        <div class="topic_title">{{ topic.title }}</div>
        <div class="topic_sub">{{ topic.subtitle }}</div>
      </a>
    </div>
  </div>
</template>

<script>
import { getHome, goodsCategory, couponReceive } from '@/api/api';
import scrollFixed from '@/mixin/scroll-fixed';
import _ from 'lodash';

import { Swipe, SwipeItem, Search, Tag, showSuccessToast } from 'vant';

export default {
  name: 'tabbar-home',
  mixins: [scrollFixed],

  data() {
    return {
      shopInfos: [],
      isLoading: false
    };
  },

  created() {
    this.initViews();
  },

  methods: {
    goDetail(id) {
      return `#/items/detail/${id}`;
    },
    goBrand(id) {
      return `#/items/brand/${id}`;
    },
    goTopic(id) {
      return `#/items/topic/${id}`;
    },
    getCoupon(id) {
      couponReceive({ couponId: id }).then(res => {
        showSuccessToast('领取成功');
      });
    },
    changeTabbar(o) {
      goodsCategory({ id: o.id }).then(res => {
        let categoryId = res.data.data.currentCategory.id;
        this.$router.replace({
          name: 'category',
          query: { itemClass: categoryId }
        });
      });
    },
    initViews() {
      getHome().then(res => {
        this.shopInfos = res.data.data;
      });
    }
  },

  components: {
    [Search.name]: Search,
    [Swipe.name]: Swipe,
    [SwipeItem.name]: SwipeItem,
    [Tag.name]: Tag
  }
};
</script>


<style lang="scss" scoped>
.tab_home {
  padding-bottom: var(--space-6);
}

.home_search {
  padding: var(--space-2) var(--space-2) 0;
}

.home_banner {
  margin: 0 var(--space-4);
  border-radius: var(--radius-card);
  overflow: hidden;
  box-shadow: var(--shadow-1);
  .banner_img {
    display: block;
    width: 100%;
    aspect-ratio: 16 / 9;
    object-fit: cover;
  }
}

.kingkong {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  row-gap: var(--space-4);
  padding: var(--space-4) var(--space-2) 0;
  .kk_item {
    text-align: center;
    cursor: pointer;
  }
  .kk_tile {
    width: 44px;
    height: 44px;
    margin: 0 auto;
    border-radius: var(--radius-card);
    background: var(--brand-soft);
    display: flex;
    align-items: center;
    justify-content: center;
    img {
      width: 24px;
      height: 24px;
      object-fit: contain;
    }
  }
  span {
    display: block;
    margin-top: var(--space-2);
    font-size: 12px;
    color: var(--text-2);
  }
}

.sec_head {
  display: flex;
  align-items: center;
  padding: var(--space-6) var(--space-4) var(--space-3);
  &::before {
    content: '';
    width: 4px;
    height: 16px;
    border-radius: 2px;
    background: var(--brand);
    margin-right: var(--space-2);
  }
  .sec_title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-1);
  }
  .sec_more {
    margin-left: auto;
    font-size: 12px;
    color: var(--text-3);
  }
}

.ticket_row {
  display: flex;
  gap: var(--space-3);
  overflow-x: auto;
  padding: 0 var(--space-4) var(--space-1);
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }
}

.ticket {
  position: relative;
  display: flex;
  align-items: center;
  min-width: 250px;
  background: var(--card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-1);
  padding: var(--space-3);
  cursor: pointer;
  &::before,
  &::after {
    content: '';
    position: absolute;
    left: 82px;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--bg);
  }
  &::before { top: -5px; }
  &::after { bottom: -5px; }
  .ticket_amount {
    width: 74px;
    flex: none;
    font-size: 26px;
    font-weight: 700;
    color: var(--brand);
    font-variant-numeric: tabular-nums;
    .cur { font-size: 13px; }
  }
  .ticket_body {
    flex: 1;
    min-width: 0;
    border-left: 1px dashed var(--line);
    padding-left: var(--space-3);
  }
  .ticket_name {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-1);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .ticket_desc {
    font-size: 11px;
    color: var(--text-3);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .ticket_btn {
    flex: none;
    margin-left: var(--space-2);
    padding: 4px 10px;
    border-radius: var(--radius-pill);
    background: var(--brand);
    color: #fff;
    font-size: 12px;
  }
}

.hcard_list {
  padding: 0 var(--space-4);
}

.hcard {
  display: flex;
  gap: var(--space-3);
  background: var(--card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-1);
  padding: var(--space-3);
  margin-bottom: var(--space-3);
  img {
    width: 88px;
    height: 88px;
    flex: none;
    border-radius: var(--radius-img);
    object-fit: cover;
  }
  .hcard_body {
    flex: 1;
    min-width: 0;
  }
  .hcard_title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-1);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .hcard_tags {
    padding: var(--space-1) 0;
    display: flex;
    gap: var(--space-1);
  }
}

.price_row {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
}

.price_now {
  font-size: 16px;
  font-weight: 700;
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}

.price_origin {
  font-size: 12px;
  color: var(--text-3);
  text-decoration: line-through;
  font-weight: 400;
}

.hscroll_row {
  display: flex;
  gap: var(--space-3);
  overflow-x: auto;
  padding: 0 var(--space-4) var(--space-1);
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }
}

.brand_card {
  flex: none;
  width: 132px;
  background: var(--card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-1);
  padding: var(--space-2);
  text-align: center;
  img {
    width: 100%;
    height: 76px;
    object-fit: cover;
    border-radius: var(--radius-img);
  }
  span {
    display: block;
    padding: var(--space-2) 0 var(--space-1);
    font-size: 13px;
    color: var(--text-1);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.gcard_grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3);
  padding: 0 var(--space-4);
}

.gcard {
  background: var(--card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-1);
  overflow: hidden;
  img {
    display: block;
    width: 100%;
    aspect-ratio: 1 / 1;
    object-fit: cover;
  }
  .gcard_name {
    padding: var(--space-2) var(--space-2) 0;
    font-size: 13px;
    color: var(--text-1);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    min-height: 38px;
  }
  .gcard_price {
    padding: var(--space-1) var(--space-2) var(--space-3);
    font-size: 16px;
    font-weight: 700;
    color: var(--brand);
    font-variant-numeric: tabular-nums;
    display: flex;
    align-items: baseline;
    gap: var(--space-2);
  }
}

.topic_card {
  flex: none;
  width: 200px;
  background: var(--card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-1);
  padding: var(--space-2);
  img {
    width: 100%;
    height: 100px;
    object-fit: cover;
    border-radius: var(--radius-img);
  }
  .topic_title {
    padding-top: var(--space-2);
    font-size: 13px;
    font-weight: 600;
    color: var(--text-1);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .topic_sub {
    font-size: 11px;
    color: var(--text-3);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    padding-bottom: var(--space-1);
  }
}
</style>