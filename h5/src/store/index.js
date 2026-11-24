import { createStore } from 'vuex'
import * as getters from './getters'
import mutations from './mutations'

const state = {
  showHeader:true,
  title:'' 
}

export default createStore({
  strict: import.meta.env.DEV,
  state,
  getters,
  mutations
})