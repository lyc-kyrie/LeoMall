<template>
  <div id="app">
    <el-config-provider :locale="epLocale" :size="$store.getters.size">
      <router-view/>
    </el-config-provider>
  </div>
</template>

<script>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'

export default{
  name: 'App',
  computed: {
    epLocale() {
      return this.$i18n.locale === 'en' ? en : zhCn
    }
  }
}
</script>
