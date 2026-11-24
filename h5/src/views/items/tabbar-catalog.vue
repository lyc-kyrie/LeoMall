<template>
  <div class="tab_class">
    <div class="tal_class_searchBox">
      <van-search shape="round" placeholder="点击前往搜索"/>
      <div class="tal_class_searchMask" @click="$router.push({ name: 'search' })"></div>
    </div>

    <div class="class_tree">
      <ul class="class_tree_nav">
        <li
          v-for="(item, index) in categoryList"
          :key="index"
          :class="{active_nav: currentCategory.id == item.id}"
          @click="changeCatalog(item.id)"
        >{{item.name}}</li>
      </ul>
      <div class="class_tree_content">
        <SkeletonBlock v-if="state === 'loading'" banner :cols="3"/>
        <ErrorState v-else-if="state === 'error'" :message="errMsg" @retry="retry"/>
        <template v-else>
          <div class="cat_banner">
            <div class="cat_banner_name">{{currentCategory.name}}</div>
            <div class="cat_banner_desc">{{currentCategory.desc}}</div>
          </div>
          <EmptyState v-if="!currentSubCategoryList.length" text="该类目下暂无子类" icon="apps-o"/>
          <div v-else class="cat_grid">
            <div class="cat_cell"
                 v-for="item in currentSubCategoryList"
                 :key="item.id"
                 @click="toItemList(item.id)">
              <div class="cat_cell_img"><img :src="item.picUrl" :alt="item.name"></div>
              <div class="cat_cell_name">{{item.name}}</div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import { catalogList, catalogCurrent } from '@/api/api';
import { Search } from 'vant';

export default {
  name: 'tabbar-catalog',
  data() {
    return {
      categoryList: [],
      currentCategory: {},
      currentSubCategoryList: [],
      state: 'loading',
      errMsg: '',
      retryId: null
    };
  },

  created() {
    this.initData();
  },

  methods: {
    initData() {
      this.state = 'loading';
      this.retryId = null;
      catalogList({ silentError: true }).then(res => {
        let data = res.data.data;
        this.categoryList = data.categoryList;
        this.currentCategory = data.currentCategory;
        this.currentSubCategoryList = data.currentSubCategory;
        this.state = 'ready';
      }).catch(err => {
        this.state = 'error';
        this.errMsg = (err && err.data && err.data.errmsg) || '分类加载失败，请重试';
      });
    },
    changeCatalog(id) {
      this.state = 'loading';
      this.retryId = id;
      catalogCurrent({ id: id }, { silentError: true }).then(res => {
        let data = res.data.data;
        this.currentCategory = data.currentCategory;
        this.currentSubCategoryList = data.currentSubCategory;
        this.state = 'ready';
      }).catch(err => {
        this.state = 'error';
        this.errMsg = (err && err.data && err.data.errmsg) || '分类加载失败，请重试';
      });
    },
    retry() {
      if (this.retryId) this.changeCatalog(this.retryId);
      else this.initData();
    },
    toItemList(id) {
      this.$router.push({
        name: 'category',
        query: { keyword: '', itemClass: id }
      });
    }
  },
  components: {
    [Search.name]: Search
  }
};
</script>

<style lang="scss" scoped>
.tab_class {
  background-color: var(--bg);
}

.tal_class_searchBox {
  position: relative;
  background: #fff;

  :deep(.van-search) {
    padding: 8px 12px;
  }
  :deep(.van-search__content) {
    background: var(--bg);
  }
}

.tal_class_searchMask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 9;
}

.class_tree {
  display: flex;
  height: calc(100vh - 104px);
  overflow: hidden;
}

.class_tree_nav {
  width: 96px;
  flex-shrink: 0;
  background-color: var(--bg);
  overflow-y: auto;
  padding: 4px 0;

  > li {
    height: 44px;
    line-height: 44px;
    margin: 4px 8px;
    border-radius: 8px;
    text-align: center;
    font-size: 13px;
    color: var(--text-2);
  }
  > li.active_nav {
    background-color: #fff;
    color: var(--brand);
    font-weight: 600;
  }
}

.class_tree_content {
  flex: 1;
  overflow-y: auto;
  background: #fff;
  padding: 12px 12px 60px;
}

.cat_banner {
  background: var(--brand-soft);
  border-radius: 12px;
  padding: 14px 16px;
}

.cat_banner_name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-1);
}

.cat_banner_desc {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 4px;
}

.cat_grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 12px;
}

.cat_cell {
  background: #fff;
  border-radius: 8px;
  padding: 8px 6px;
  text-align: center;
}

.cat_cell_img {
  aspect-ratio: 1 / 1;
  background: var(--bg);
  border-radius: 8px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
    display: block;
  }
}

.cat_cell_name {
  font-size: 12px;
  color: var(--text-1);
  margin-top: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>