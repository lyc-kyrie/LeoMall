<template>
  <div class="user_header">
    <van-icon name="setting-o" class="user_set" @click="toSetting" />
    <div class="user_avatar">
      <img :src="avatar" alt="头像" width="56" height="56">
    </div>
    <div class="user_nick">{{ nickName }}</div>
  </div>
</template>

<script>
// 阶段2D-3：红渐变头（同色相浅→深）+ 白字 + 白环头像，替代外链背景图
import avatar_default from '@/assets/images/avatar_default.png';
import { getLocalStorage } from '@/utils/local-storage';

export default {
  name: 'user-header',

  props: {
    isLogin: {
      type: Boolean,
      default: false
    }
  },

  data() {
    return {
      nickName: '昵称',
      avatar: avatar_default
    };
  },

  activated() {
    this.getUserInfo();
  },

  methods: {
    getUserInfo() {
      const infoData = getLocalStorage(
        'nickName',
        'avatar'
      );
      this.avatar = infoData.avatar || avatar_default;
      this.nickName = infoData.nickName || '昵称';
    },
    toSetting() {
      this.$router.push({ name: 'user-information' });
    }
  }
};
</script>

<style lang="scss" scoped>
.user_header {
  position: relative;
  height: 150px;
  text-align: center;
  color: #fff;
  padding-top: 34px;
  background: linear-gradient(160deg, #E4574F 0%, var(--brand) 55%, var(--brand-deep) 100%);
}

.user_set {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 22px;
  color: #fff;
}
.user_avatar {
  margin-bottom: 8px;
  img {
    border: 2px solid rgba(255, 255, 255, 0.85);
    border-radius: 50%;
    background: var(--card);
  }
}
.user_nick {
  font-size: 16px;
  font-weight: 600;
}
</style>