<template>
  <div class="navbar">
    <hamburger :toggle-click="toggleSideBar" :is-active="sidebar.opened" class="hamburger-container" />

    <breadcrumb class="breadcrumb-container" />

    <div class="right-menu">
      <template v-if="device!=='mobile'">
        <el-tooltip :content="$t('navbar.tooltip.full_screen')" effect="dark" placement="bottom">
          <screenfull class="right-menu-item" />
        </el-tooltip>

        <el-tooltip :content="$t('navbar.tooltip.size_select')" effect="dark" placement="bottom">
          <size-select class="right-menu-item" />
        </el-tooltip>

        <el-tooltip :content="$t('navbar.tooltip.notice')" effect="dark" placement="bottom">
          <notice class="right-menu-item" />
        </el-tooltip>
      </template>

      <el-dropdown class="avatar-container right-menu-item" trigger="click">
        <div class="avatar-wrapper">
          <span class="user-avatar">{{ (name || 'A').slice(0, 1).toUpperCase() }}</span>
          <span class="user-name">{{ name }}</span>
          <el-icon class="avatar-caret"><ArrowDown /></el-icon>
        </div>
        <template #dropdown><el-dropdown-menu>
          <router-link to="/">
            <el-dropdown-item>
              {{ $t('navbar.menu.home') }}
            </el-dropdown-item>
          </router-link>
          <el-dropdown-item divided>
            <router-link to="/profile/password">
              {{ $t('navbar.menu.password') }}
            </router-link>
          </el-dropdown-item>
          <el-dropdown-item divided @click="logout">
            <span style="display:block;">{{ $t('navbar.menu.logout') }}</span>
          </el-dropdown-item>
        </el-dropdown-menu></template>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import Hamburger from '@/components/Hamburger'
import Screenfull from '@/components/Screenfull'
import SizeSelect from '@/components/SizeSelect'
import Notice from '@/components/Notice'

export default {
  components: {
    Breadcrumb,
    Hamburger,
    Screenfull,
    SizeSelect,
    Notice
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'name',
      'avatar',
      'device'
    ])
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('toggleSideBar')
    },
    logout() {
      this.$store.dispatch('LogOut').then(() => {
        this.$router.push(`/login?redirect=${this.$route.fullPath}`)
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.navbar {
  height: 50px;
  line-height: 50px;
  border-radius: 0px !important;
  background: #fff;
  border-bottom: 1px solid var(--line, #e5e6eb);
  .hamburger-container {
    line-height: 58px;
    height: 50px;
    float: left;
    padding: 0 10px;
  }
  .breadcrumb-container{
    float: left;
  }
  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }
  .right-menu {
    float: right;
    height: 100%;
    &:focus{
     outline: none;
    }
    .right-menu-item {
      display: inline-block;
      margin: 0 8px;
      vertical-align: top;
      color: var(--text-2, #4e5969);
    }
    .avatar-container {
      height: 50px;
      margin-right: 20px;
      .avatar-wrapper {
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 8px;
        height: 50px;
        .user-avatar {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          background: #e8f4ff;
          color: #1890ff;
          font-size: 14px;
          font-weight: 600;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .user-name {
          font-size: 13px;
          color: var(--text-1, #1f2329);
          line-height: 1;
        }
        .avatar-caret {
          font-size: 12px;
          color: var(--text-3, #86909c);
        }
      }
    }
  }
}
</style>
