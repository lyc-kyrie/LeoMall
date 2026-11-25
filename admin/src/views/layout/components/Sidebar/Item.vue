<script>
// 阶段2D-4a：Vue2 functional+JSX → Vue3 普通 render(h)（免 JSX 依赖）
// 注意：render 里字符串标签是原生元素，组件必须传组件对象（h('svg-icon') 不会解析全局注册）
import { h } from 'vue'
import SvgIcon from '@/components/SvgIcon'

export default {
  name: 'MenuItem',
  props: {
    icon: {
      type: String,
      default: ''
    },
    title: {
      type: String,
      default: ''
    }
  },
  render() {
    const vnodes = []
    if (this.icon) {
      vnodes.push(h(SvgIcon, { iconClass: this.icon }))
    }
    if (this.title) {
      vnodes.push(h('span', {}, this.title))
    }
    return vnodes
  }
}
</script>

<style scoped>
.sub-el-icon {
  color: currentColor;
  width: 1em;
  height: 1em;
}
</style>