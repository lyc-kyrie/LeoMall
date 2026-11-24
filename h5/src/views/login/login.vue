<template>
	<div class="login">
		<div class="brand_header">
			<div class="brand_mark">L</div>
			<div class="brand_name">LeoMall</div>
			<div class="brand_slogan">品质生活 · 居家严选</div>
		</div>

		<div class="login_card">
			<md-field-group>
				<md-field
					v-model="account"
					icon="username"
					placeholder="请输入账号"
					right-icon="clear-full"
					name="user"
					@right-click="clearText"
				/>

				<md-field
					v-model="password"
					icon="lock"
					placeholder="请输入密码"
					:type="visiblePass ? 'text' : 'password'"
					:right-icon="visiblePass ? 'eye-open' : 'eye-close'"
					name="password"
					@right-click="visiblePass = !visiblePass"
				/>

				<div class="link_row clearfix">
					<div class="float-l">
						<router-link to="/login/registerGetCode">免费注册</router-link>
					</div>
					<div class="float-r">
						<router-link to="/login/forget">忘记密码</router-link>
					</div>
				</div>

				<van-button round block type="danger" :loading="isLogining" @click="loginSubmit">登录</van-button>
			</md-field-group>
		</div>

		<div class="text-desc text-center login_footer">技术支持: LeoMall</div>
	</div>
</template>

<script>
import field from '@/components/field/';
import fieldGroup from '@/components/field-group/';

import { authLoginByAccount } from '@/api/api';
import { setLocalStorage } from '@/utils/local-storage';
import { emailReg, mobileReg } from '@/utils/validate';

import { showFailToast } from 'vant';


export default {
  name: 'login-request',
  components: {
    [field.name]: field,
    [fieldGroup.name]: fieldGroup
  },
  data() {
    return {
      account: '',
      password: '',
      visiblePass: false,
      isLogining: false,
      userInfo: {}
    };
  },

  methods: {
    clearText() {
      this.account = '';
    },

    validate() {

    },

    login() {
      let loginData = this.getLoginData();
      authLoginByAccount(loginData).then(res => {
        this.userInfo = res.data.data.userInfo;
        setLocalStorage({
          Authorization: res.data.data.token,
          avatar: this.userInfo.avatarUrl,
          nickName: this.userInfo.nickName
        });

        this.routerRedirect();
      }).catch(error => {
        showFailToast(error.data.errmsg);
      });
    },

    loginSubmit() {
      this.isLogining = true;
      try {
        this.validate();
        this.login();
        this.isLogining = false;
      } catch (err) {
        console.log(err.message);
        this.isLogining = false;
      }
    },

    routerRedirect() {
      window.location = '#/user/';
    },

    getLoginData() {
      const password = this.password;
      const account = this.getUserType(this.account);
      return {
        [account]: this.account,
        password: password
      };
    },

    getUserType(account) {
      const accountType = mobileReg.test(account)
        ? 'mobile'
        : emailReg.test(account)
        ? 'email'
        : 'username';
      return accountType;
    }
  }
};
</script>


<style lang="scss" scoped>
.login {
  position: relative;
  min-height: 100vh;
  background: var(--bg);
  padding: var(--space-8) var(--space-4) var(--space-6);
}

.brand_header {
  text-align: center;
  padding: var(--space-6) 0 var(--space-8);
  .brand_mark {
    width: 64px;
    height: 64px;
    margin: 0 auto;
    border-radius: 18px;
    background: linear-gradient(135deg, var(--brand), var(--brand-deep));
    color: #fff;
    font-size: 34px;
    font-weight: 700;
    line-height: 64px;
    box-shadow: var(--shadow-2);
  }
  .brand_name {
    padding-top: var(--space-3);
    font-size: 22px;
    font-weight: 700;
    color: var(--text-1);
    letter-spacing: .5px;
  }
  .brand_slogan {
    padding-top: var(--space-1);
    font-size: 13px;
    color: var(--text-3);
  }
}

.login_card {
  background: var(--card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-1);
  padding: var(--space-6) var(--space-4);
}

.link_row {
  padding: var(--space-3) 0 var(--space-4);
  a {
    font-size: 13px;
    color: var(--text-3);
  }
}

.login_footer {
  padding-top: var(--space-8);
  color: var(--text-3);
}
</style>