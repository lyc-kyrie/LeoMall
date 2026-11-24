<template>
	<div>
		<van-cell-group>
			<van-field
				label="登录密码"
				v-model="password"
				type="password"
				placeholder="请输入登录密码"
				 />

			<van-field
				label="新手机号"
				v-model="mobile"
				placeholder="请输入新手机号"
				/>

			<van-field
				label="验证码"
				v-model="code"
				@click-icon="getCode"
				placeholder="请输入验证码">

				<template #button>
					<span
						class="verifi_code red"
						:class="{verifi_code_counting: counting}"
						@click="getCode">
						<countdown v-if="counting" :time="60000" @end="countdownend">
						  <template #default="props">{{ +props.seconds || 60 }}秒后获取</template>
						</countdown>
						<span v-else>获取验证码</span>
					</span>
				</template>
			</van-field>
		</van-cell-group>

		<div class="bottom_btn">
			<van-button size="large" type="danger" @click="saveMobile">保存</van-button>
		</div>
	</div>
</template>


<script>
import { authCaptcha } from '@/api/api';

import { Field, showSuccessToast, showFailToast } from 'vant';

export default {
  data: () => ({
    password: '',
    mobile: '',
    code: '',
    counting: false
  }),

  methods: {
    getCode() {
      if (!this.counting && this.vuelidate()) {
        authCaptcha({
          mobile: this.mobile,
          type: 'bind-mobile'
        }).then(() => {
          showSuccessToast('发送成功');
          this.counting = true;
        }).catch(error => {
          showFailToast(error.data.errmsg);
          this.counting = false;
        })

      }
    },
    countdownend() {
      this.counting = false;
    },
    vuelidate() {
      if(this.mobile === ''){
        showFailToast('请输入号码');
        return false;
      }
      return true;
    },
    saveMobile() {
      console.log('保存手机号');
    }
  },

  components: {
    [Field.name]: Field
  }
};
</script>

<style lang="scss" scoped>
@import '../../../../assets/scss/var';
@import '../../../../assets/scss/mixin';
.bottom_btn {
  padding: 30px 15px 0 15px;
}

.verifi_code {
  @include one-border;
  padding-left: 10px;
  &::after {
    border-bottom: 0;
    border-left: 1px solid $border-color;
  }

  &_counting {
    color: $font-color-gray;
  }
}
</style>
