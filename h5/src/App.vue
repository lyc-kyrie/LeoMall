<template>
<div id="app">
  <v-header></v-header>
  <router-view v-slot="{ Component }">
    <keep-alive :include="['tabbar-home', 'tabbar-catalog', 'search-view', 'tabbar-user']">
      <component :is="Component" class="view-router" />
    </keep-alive>
  </router-view>
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