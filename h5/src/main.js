import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import store from './store';

import Vant, { Lazyload } from 'vant';
import 'vant/lib/index.css';
import '@tokens';
import '@/assets/scss/global.scss';
import '@/assets/scss/iconfont/iconfont.css';

import VueCountdown from '@chenfengyuan/vue-countdown';
import { yuan, dateFormat } from '@/filter';
import goodsDefaultImg from '@/assets/images/goods_default.png';

const app = createApp(App);

// Vue3 移除 filters：改为全局方法，模板 {{ yuan(x) }} 调用
app.config.globalProperties.yuan = yuan;
app.config.globalProperties.dateFormat = dateFormat;

app.component('VueCountdown', VueCountdown);
app.component('countdown', VueCountdown);

app.use(store);
app.use(router);
app.use(Vant);
app.use(Lazyload, {
  preLoad: 1.3,
  error: goodsDefaultImg,
  loading: goodsDefaultImg,
  attempt: 1,
  listenEvents: ['scroll'],
  lazyComponent: true
});

app.mount('#app');