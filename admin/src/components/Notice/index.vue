<template>
  <div>
    <el-badge :is-dot="hasNotice">
      <el-icon class="notice-bell" @click="click"><Bell /></el-icon>
    </el-badge>
  </div>
</template>

<script>
import { nNotice } from '@/api/profile'

export default {
  data() {
    return {
      hasNotice: false,
      timer: ''
    }
  },
  mounted() {
    this.timer = setInterval(this.checkNotice, 30 * 1000)
  },
  beforeUnmount() {
    clearInterval(this.timer)
  },
  created() {
    this.checkNotice()
  },
  methods: {
    click() {
      this.$router.push({ path: '/profile/notice' })
    },
    checkNotice() {
      if (this.hasNotice) {
        return
      }
      nNotice().then(response => {
        this.hasNotice = response.data.data > 0
      })
    }
  }
}
</script>

<style lang="scss" scoped>
:deep(.el-badge__content.is-fixed.is-dot) {
  right: 5px;
  top: 10px;
}

.notice-bell {
  font-size: 20px;
  cursor: pointer;
}
</style>
