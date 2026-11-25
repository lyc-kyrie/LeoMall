import { createApp } from 'vue'
import Cookies from 'js-cookie'

import 'normalize.css/normalize.css'

import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementIcons from '@element-plus/icons-vue'

import '@tokens' // 阶段2D：tokens 单源（仓库根 design/tokens.css）
import '@/styles/index.scss' // global css
import '@/styles/element-theme.scss' // Element Plus CSS variables 映射（替代编译期 SCSS 主题）

import App from './App'
import store from './store'
import router from './router'

import './icons' // icon
import './permission' // permission control

import permission from '@/directive/permission' // 权限判断指令
import clipboard from '@/directive/clipboard'
import Print from '@/utils/print' // 打印
import SvgIcon from '@/components/SvgIcon'

import { createI18n } from 'vue-i18n'
import zhHans from '@/locales/zh-Hans'
import en from '@/locales/en'

const i18n = createI18n({
  legacy: true,
  locale: Cookies.get('language') || 'zh-Hans',
  fallbackLocale: 'zh-Hans',
  messages: { 'zh-Hans': zhHans, en }
})

const app = createApp(App)

// 阶段2D-4a：Vue3 废除 filters；模板管道已改方法调用，此处把组件 filters 绑到实例供调用
app.mixin({
  beforeCreate() {
    const f = this.$options.filters
    if (f) {
      Object.keys(f).forEach(k => { this[k] = f[k].bind(this) })
    }
  }
})

// Element Plus 图标全局注册（icon="Search" 字符串用法与 <Search /> 皆可）
Object.entries(ElementIcons).forEach(([name, comp]) => app.component(name, comp))
app.component('svg-icon', SvgIcon)

// EP size 仅 default/small/large（element-ui 的 medium/mini 已废除）
const cookieSize = Cookies.get('size')
const size = ['default', 'small', 'large'].includes(cookieSize) ? cookieSize : 'default'

app.use(store)
app.use(router)
app.use(i18n)
app.use(ElementPlus, { locale: zhCn, size })

app.directive('permission', permission)
app.directive('clipboard', clipboard)
app.use(Print)

app.mount('#app')