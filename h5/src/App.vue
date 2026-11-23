<template>
<div id="app">
  <v-header></v-header>
  <keep-alive>
    <router-view class="view-router"  v-if="$route.meta.keepAlive"></router-view>
  </keep-alive>
  <router-view class="view-router" v-if="!$route.meta.keepAlive"></router-view>
  <router-view name="tabbar"></router-view>
  <agent-chat></agent-chat>
</div>
</template>
<script>
import header from "@/components/Header";
import AgentChat from '@/components/AgentChat';

export default {
  components:{
    'v-header': header,
    'agent-chat': AgentChat
  }
}
</script>
<style lang="scss" src="./assets/scss/global.scss" />
