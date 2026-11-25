<template>
  <div class="login-container">
    <div class="locale-changer">
      <locale-changer />
    </div>

    <div class="login-card">
      <div class="login-brand">
        <div class="login-brand-mark">L</div>
        <div class="login-brand-text">
          <div class="login-brand-name">LeoMall 管理端</div>
          <div class="login-brand-sub">智能商城运营后台</div>
        </div>
      </div>

      <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form" autocomplete="on" label-position="top">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" name="username" type="text" tabindex="1" autocomplete="on" size="large" :placeholder="$t('login.placeholder.username')">
            <template #prefix><svg-icon icon-class="user" /></template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input v-model="loginForm.password" :type="passwordType" name="password" autocomplete="on" tabindex="2" size="large" show-password :placeholder="$t('login.placeholder.password')" @keyup.enter="handleLogin">
            <template #prefix><svg-icon icon-class="password" /></template>
          </el-input>
        </el-form-item>

        <el-form-item prop="code">
          <div class="login-code-row">
            <el-input v-model="loginForm.code" autocomplete="off" name="code" tabindex="3" size="large" placeholder="验证码" @keyup.enter="handleLogin">
              <template #prefix><svg-icon icon-class="lock" /></template>
            </el-input>
            <img class="login-code-img" :src="codeImg" title="点击刷新" @click="getCode">
          </div>
        </el-form-item>

        <el-button :loading="loading" type="primary" size="large" class="login-submit" @click.prevent="handleLogin">{{ $t('login.button.login') }}</el-button>
      </el-form>
    </div>

    <div class="copyright">© 2026 LeoMall</div>
  </div>
</template>

<script>
import { getKaptcha } from '@/api/login'
import LocaleChanger from '@/components/LocaleChanger'

export default {
  name: 'Login',
  components: { LocaleChanger },
  data() {
    const validatePassword = (rule, value, callback) => {
      if (value.length < 6) {
        callback(new Error('管理员密码长度应大于6'))
      } else {
        callback()
      }
    }
    return {
      loginForm: {
        username: '',
        password: '',
        code: ''
      },
      codeImg: '',
      loginRules: {
        username: [{ required: true, message: '管理员账户不允许为空', trigger: 'blur' }],
        password: [
          { required: true, message: '管理员密码不允许为空', trigger: 'blur' },
          { validator: validatePassword, trigger: 'blur' }
        ],
        code: [{ required: true, message: '验证码不允许为空', trigger: 'blur' }]
      },
      passwordType: 'password',
      loading: false
    }
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }

  },
  created() {
    this.getCode()
  },
  methods: {
    getCode() {
      getKaptcha().then(response => {
        this.codeImg = response.data.data
      })
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid && !this.loading) {
          this.loading = true
          this.$store.dispatch('LoginByUsername', this.loginForm).then(() => {
            this.loading = false
            this.$router.push({ path: this.redirect || '/' })
          }).catch(response => {
            if (response.data.data) {
              this.codeImg = response.data.data
            }
            this.$notify.error({
              title: '失败',
              message: response.data.errmsg
            })
            this.loading = false
          })
        } else {
          return false
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
/* 阶段2D-4b：浅色 SaaS 登录卡（主色沿用原配置蓝 #1890ff） */
.login-container {
  min-height: 100%;
  width: 100%;
  background:
    radial-gradient(600px 300px at 15% 0%, rgba(24, 144, 255, 0.08), transparent 60%),
    radial-gradient(600px 300px at 85% 100%, rgba(24, 144, 255, 0.06), transparent 60%),
    var(--bg, #f7f8fa);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;

  .locale-changer {
    position: absolute;
    top: 16px;
    right: 16px;
  }

  .login-card {
    width: 400px;
    max-width: calc(100% - 32px);
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 8px 32px rgba(31, 35, 41, 0.08);
    padding: 32px 32px 24px;
  }

  .login-brand {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 28px;
    .login-brand-mark {
      width: 40px;
      height: 40px;
      border-radius: 12px;
      background: #1890ff;
      color: #fff;
      font-size: 20px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      flex: none;
    }
    .login-brand-name {
      font-size: 18px;
      font-weight: 600;
      color: var(--text-1, #1f2329);
      line-height: 1.2;
    }
    .login-brand-sub {
      font-size: 12px;
      color: var(--text-3, #86909c);
      margin-top: 2px;
    }
  }

  .login-form {
    :deep(.el-input__wrapper) {
      border-radius: 8px;
    }
    :deep(.svg-icon) {
      color: var(--text-3, #86909c);
    }
  }

  .login-code-row {
    display: flex;
    gap: 10px;
    width: 100%;
    .el-input {
      flex: 1;
    }
    .login-code-img {
      width: 108px;
      height: 40px;
      border-radius: 8px;
      border: 1px solid var(--line, #e5e6eb);
      cursor: pointer;
      object-fit: cover;
      flex: none;
    }
  }

  .login-submit {
    width: 100%;
    border-radius: 8px;
    font-weight: 600;
    letter-spacing: 2px;
  }

  .copyright {
    font-size: 12px;
    color: var(--text-3, #86909c);
    position: absolute;
    bottom: 16px;
    left: 50%;
    transform: translateX(-50%);
    letter-spacing: 0.6px;
  }
}
</style>