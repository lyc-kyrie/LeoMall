<template>
  <div class="item_search">
    <form action="/search" @submit="disabledSubmit">
      <van-search shape="round"
                  placeholder="请输入商品名称"
                  v-model="keyword"
                  @search="enterSearch"
                  autofocus />
    </form>

    <div class="item_search_content" v-if="!searched">
      <div class="sec" v-if="hotWords.length">
        <div class="sec_head">热搜</div>
        <div class="tag_row">
          <span class="kw_tag kw_tag_hot"
                v-for="w in hotWords"
                :key="w.id"
                @click="clickSearch(w.keyword)">{{w.keyword}}</span>
        </div>
      </div>
      <div class="sec" v-if="wordHistory.length">
        <div class="sec_head">历史搜索
          <van-icon name="delete-o" class="clear_icon" @click="clearHistory"/>
        </div>
        <div class="tag_row">
          <span class="kw_tag"
                v-for="(his, i) in wordHistory"
                :key="i"
                @click="clickSearch(his)">{{his}}</span>
        </div>
      </div>
      <EmptyState v-if="!hotWords.length && !wordHistory.length" text="输入关键词开始搜索" icon="search"/>
    </div>

    <div class="item_search_result" v-else>
      <SkeletonBlock v-if="state === 'loading' && !list.length" :cols="2"/>
      <ErrorState v-else-if="state === 'error'" :message="errMsg" @retry="retrySearch"/>
      <EmptyState v-else-if="state === 'ready' && !list.length" text="没有找到相关商品，换个词试试" icon="search"/>
      <van-list v-else
                v-model:loading="loading"
                :finished="finished"
                :immediate-check="false"
                @load="loadMore">
        <div class="result_grid">
          <GoodsCard v-for="item in list" :key="item.id" :goods="item" @click="itemClick"/>
        </div>
      </van-list>
    </div>
  </div>
</template>

<script>
import { Search, List, showConfirmDialog } from 'vant';
import { goodsList, searchIndex } from '@/api/api';
import GoodsCard from '@/components/goods-card/index.vue';

export default {
  name: 'search-view',
  data() {
    return {
      keyword: '',
      wordHistory: [],
      hotWords: [],
      list: [],
      page: 1,
      limit: 10,
      pages: 0,
      loading: false,
      finished: false,
      searched: false,
      state: 'idle',
      errMsg: ''
    };
  },
  methods: {
    enterSearch() {
      if (!this.keyword.trim()) return;
      this.pushHistoryTolocal(this.keyword.trim());
      this.wordHistory = this.getKeyWordHistory();
      this.searched = true;
      this.reset();
      this.searchGoods();
    },
    clickSearch(word) {
      this.keyword = word.trim();
      this.enterSearch();
    },
    reset() {
      this.list = [];
      this.page = 1;
      this.limit = 10;
      this.pages = 0;
      this.loading = false;
      this.finished = false;
      this.state = 'loading';
      this.errMsg = '';
    },
    pushHistoryTolocal(keyword) {
      const wordHistory = this.getKeyWordHistory();
      if (!!keyword && wordHistory.indexOf(keyword) < 0) {
        wordHistory.unshift(keyword);
        window.localStorage.setItem('keyword', wordHistory.slice(0, 10).join('|'));
      }
    },
    getKeyWordHistory() {
      const listWord = window.localStorage.getItem('keyword');
      return listWord ? listWord.split('|').filter(w => !!w) : [];
    },
    clearHistory() {
      showConfirmDialog({ message: '是否清空历史记录' })
        .then(() => {
          window.localStorage.setItem('keyword', '');
          this.wordHistory = [];
        })
        .catch(() => {});
    },
    disabledSubmit() {
      return false;
    },
    searchGoods() {
      if (!this.list.length) this.state = 'loading';
      return goodsList({
        keyword: this.keyword,
        page: this.page,
        limit: this.limit,
        categoryId: 0
      }, { silentError: true }).then(res => {
        var data = res.data.data;
        this.list.push(...data.list);
        this.page = data.page;
        this.limit = data.limit;
        this.pages = data.pages;
        this.state = 'ready';
        if (this.pages <= this.page) this.finished = true;
      }).catch(err => {
        this.state = 'error';
        this.loading = false;
        this.errMsg = (err && err.data && err.data.errmsg) || '搜索失败，请重试';
      });
    },
    retrySearch() {
      this.reset();
      this.searchGoods();
    },
    async loadMore() {
      this.page += 1;
      await this.searchGoods();
      this.loading = false;
    },
    itemClick(id) {
      this.$router.push(`/items/detail/${id}`);
    },
    loadHot() {
      searchIndex({ silentError: true }).then(res => {
        this.hotWords = (res.data.data && res.data.data.hotKeywordList) || [];
      }).catch(() => {});
    }
  },
  created() {
    this.loadHot();
  },
  activated() {
    this.wordHistory = this.getKeyWordHistory();
  },
  components: {
    [Search.name]: Search,
    [List.name]: List,
    GoodsCard
  }
};
</script>

<style lang="scss" scoped>
.item_search {
  background-color: var(--bg);
  min-height: 100vh;

  :deep(.van-search) {
    background: #fff;
    padding: 8px 12px;
  }
  :deep(.van-search__content) {
    background: var(--bg);
    border: 1px solid transparent;
  }
  :deep(.van-search__content:focus-within) {
    border-color: var(--brand);
    background: #fff;
  }
}

.sec {
  padding: 12px 16px 0;
}

.sec_head {
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.clear_icon {
  font-size: 16px;
  color: var(--text-3);
}

.tag_row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.kw_tag {
  background: #fff;
  color: var(--text-2);
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
}

.kw_tag_hot {
  background: var(--brand-soft);
  color: var(--brand);
}

.result_grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  padding: 12px 16px 60px;
}
</style>