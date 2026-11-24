# 阶段2D 前置：前端 UI 现状分析与重设计方案

> 范围：`h5/`（用户端）与 `admin/`（管理端）纯表现层；不改业务逻辑与后端接口。
> 证据：2026-11-24 本机真实运行截屏（`docs/design-shots/`）+ 源码审读。
> 定位：本方案是阶段2D（Vue3 + Vite + Vant4 + Element Plus）迁移的**设计输入**，
> 主题与组件规范随迁移一次重建，不在 Vue2 旧样式上打补丁。

---

## 一、现状盘点

### 1.1 技术栈与样式组织

| 端 | 框架 | 组件库 | 构建 | 样式组织 |
|---|---|---|---|---|
| h5 | Vue 2.5.17 | Vant 2.0.6 | vue-cli 3 / webpack4 | `assets/scss/global.scss` + `_var.scss`（$red:#db3d3c 等 6 个变量）+ `_vant-theme.scss`（**按 v2 类名覆写**）+ 各页面 scoped scss + 自定义 iconfont |
| admin | Vue 2.6.10 | Element UI 2.15.6 | vue-cli 3 / webpack4 | vue-element-admin 骨架：`styles/variables.scss`（侧栏深蓝 #304156）+ `element-variables.scss`（**编译期**全量主题 import，primary #1890ff）+ echarts 4.2.1 |

结构性问题：

1. **四套色彩并存**：H5 品牌红 #db3d3c、admin 蓝 #1890ff、swiper 指示点 Vant 默认蓝 #1989fa、
   购买条/AI 悬浮钮橙黄渐变——无统一 token，观感割裂。
2. **主题机制落后**：Element 编译期 SCSS 主题换色需重编译；Vant 覆写依赖 v2 内部类名
   （`.van-goods-action__mini-btn`、`.van-card__footer{padding-left:130px}` 等魔法数），
   **升级 Vant4 后全部失效**——迁移时必须重建而非搬运。
3. **无三态规范**：无骨架屏/空态/错误态组件。实测详情页加载期直接渲染 `¥NaN` + 灰块
   （见 `design-shots/h5-detail-loading.png`），加载完成才正常（`h5-detail2.png`）。
4. **桌面适配半成品**：H5 `body{max-width:700px}` 居中，但固定底栏、AI 悬浮钮相对**视口**定位，
   桌面宽屏下与内容壳错位；700px 也偏宽，移动质感稀释。
5. **品牌残留**：H5 登录标题 "leomall-vue"、分类页蓝色 "litemall" logo 字 + 旧 slogan；
   首页 banner 仍是演示素材（日文文案）。
6. **可访问性欠账**：灰字 #999 白底对比度约 2.8:1（<4.5:1）；无 focus-visible 规范；
   部分点击目标 <44px；无 prefers-reduced-motion 处理。

### 1.2 逐页现状问题（截屏证据）

**H5 登录**（`h5-login.png`）
- 模板默认灰色头像 + "leomall-vue" 标题，无品牌视觉；表单在桌面拉成 ~640px 宽条。
- 输入框直角细边框、图标灰淡；"免费注册/忘记密码" 纯黑小字链接；红色登录按钮直角通栏。
- 下半屏全空白，无视觉重心。

**H5 首页**（`h5-home.png`）
- **banner 图未铺满 slide**：左图右灰露底（swiper img 宽度问题），首屏即破相。
- 金刚区 10 个线性图标单行排布，图标风格不统一（iconfont 混排）。
- 优惠券 = 红描边直角矩形 ×2，无票券语义（无齿孔/无领取按钮态），信息排布松散。
- 区块标题纯文本 + 灰色厚分隔带（#f2f2f2 横条）切割页面，节奏生硬。
- 品牌商 2 列图卡直角、图下文字居中贴边；无圆角/阴影/留白体系。
- tabbar 默认 Vant 图标 + 红 active，无质感处理。

**H5 分类**（`h5-catalog.png`）
- 右上蓝色 "litemall" 字标残留 + "回家，放松身心" 旧 slogan。
- 左类目栏白底 + 红色左条 active，无图标无层级质感；右子类目 3 列小图 + 文字，
  图尺寸不一、无卡片容器，大片留白显得未完成。

**H5 详情**（`h5-detail2.png` / `h5-detail-loading.png`）
- 加载期 `¥NaN` 裸奔（无骨架/无 v-if 守卫）。
- swiper 指示点**蓝色**，与红主题冲突。
- 价格行现价/划线价紧贴无间距；标题/副标题行距局促。
- 规格/属性/运费默认 cell + 灰厚分隔带；详情图通栏直角。
- 底部购买条**黄→橙→红渐变**通栏胶囊，与全站_flat 红_冲突，且桌面下横跨全视口与 700px 壳错位。

**H5 购物车/结算/我的**（源码级，未登录截屏）
- `van-card` 默认样式 + `_vant-theme.scss` 魔法数覆写（footer padding-left:130px）。
- 我的头部、订单四宫格、模块列表均为默认 cell 堆叠，无卡片分组与品牌色头部。
- 优惠券列表与首页券同款直角描边矩形。

**AI 导购入口**（全页右下角）
- 橙红渐变圆形悬浮钮，第三套色彩；与品牌红不统一。

**admin 登录**（`admin-login.png`）
- vue-element-admin 默认深蓝底（#2d3a4b 系）+ #1890ff 按钮 + 白字标题，零品牌。
- 验证码白底图贴在深色输入行右端，生硬；语言切换悬在右上无归属。

**admin 框架与仪表盘**（源码级）
- 深蓝侧栏 #304156 + TagsView 多标签，经典但陈旧； navbar 无面包屑/全局搜索。
- **仪表盘仅 4 个 count-to 数字卡**（用户/商品/货品/订单总数），无趋势图、无待办、
  无最近订单——echarts 依赖在但 dashboard 未用；点击卡片 emit 的图表事件无承接组件。
- 列表页模板默认：筛选区拥挤、表格无斑马纹/圆角容器、空态默认 "暂无数据" 文字。

---

## 二、设计方向与 Design Tokens

**定位**：品质生活电商——保留现有"居家严选"内容基因，视觉升级为
**白卡片 + 大留白 + 克制品牌红**的现代电商质感（参考网易严选/京东首页的卡片节奏，
而非渐变促销风）。管理端走**浅色现代 SaaS** 路线，与用户端共享品牌色。

### 2.1 色彩 token（两端共享，CSS variables 承载）

| token | 值 | 用途 |
|---|---|---|
| `--brand` | #D93A3A | 品牌红（由 #db3d3c 微调）：主 CTA、active、价格 |
| `--brand-deep` | #B32E2E | 按压态/渐变深色端（仅允许同色系渐变） |
| `--brand-soft` | #FDEEEE | 品牌浅底：标签/图标底/选中行 |
| `--success` / `--warning` / `--danger` / `--info` | #22A06B / #F5A623 / #E5484D / #6B7280 | 功能色 |
| `--bg` | #F7F8FA | 页面底（替换 #f2f2f2） |
| `--card` | #FFFFFF | 卡片底 |
| `--text-1/2/3` | #1F2329 / #4E5969 / #86909C | 主/次/辅助文字（**#86909C 白底 4.6:1 达标**，替换 #999） |
| `--line` | #E5E6EB | 描边/分割（1px，替代灰厚带） |

规则：品牌红只出现在 CTA/价格/active/品牌标识；**废除跨色相渐变**（购买条、AI 钮）；
swiper 指示点、checkbox、radio、stepper 全部走 token。

### 2.2 形状 / 阴影 / 间距 / 字体 / 动效

- 圆角：卡片 12、输入/按钮 8、主 CTA 胶囊 999、图片 8（详情画廊通栏除外）。
- 阴影三级：`0 1 2px rgba(31,35,41,.04)`（卡片静置）、`0 6px 16px rgba(31,35,41,.08)`（浮层/弹层）、无（行内）。
- 间距 4 基数：4/8/12/16/24/32；页面左右 gutter 16；卡片间 12。
- 字体：系统栈 `-apple-system, "PingFang SC", "Microsoft YaHei", sans-serif`；
  字号阶 12/13/14/16/18/22/28；价格数字 `font-variant-numeric: tabular-nums` + 加粗；行高 1.5。
- 动效：150–250ms ease-out；页面 fade+4px slide；骨架 shimmer；按钮按压 scale(.98)；
  统一 `@media (prefers-reduced-motion: reduce)` 关闭。
- 暗色模式：token 已 CSS variables 化，预留 `data-theme="dark"` 映射表（2D 后期可选开关）。

### 2.3 桌面适配

- H5 内容壳 `max-width: 480px` 居中（现 700 偏宽）；壳外两侧 `--bg` + 淡品牌纹样。
- 固定底栏/悬浮钮改为**壳内定位**（`left:50%; transform:translateX(-50%); width:min(100%,480px)`），消除错位。
- admin 最小适配 1280，侧栏可折叠 64px 图标轨。

---

## 三、逐页重设计要点

### 3.1 H5（Vant4 组件映射）

| 页面 | 重设计要点 | 关键组件 |
|---|---|---|
| 登录/注册/忘记 | 品牌 logo+slogan 顶置；表单白色圆角 12 卡片+阴影；输入前缀图标+密码可见切换；主 CTA 胶囊红；次链接 `--text-3`；桌面 480 壳居中 | Field/Button/ConfigProvider |
| 首页 | banner 修铺满（img object-fit:cover）+16:9 圆角 12+品牌色指示点；金刚区 2 行×5 圆角图标底（`--brand-soft` 等浅色底）；优惠券**横滑票券卡**（齿孔+面额大字+领取按钮态）；区块标题=红短竖条+题+右"更多"箭头，**废除灰厚带**改 12 间距；品牌商横滑圆角卡；猜你喜欢双列瀑布圆角卡（1:1 图+两行题+红价+标签）；tabbar 白磨砂+红 active | Swipe/Tabbar/Skeleton |
| 分类 | 左轨 `--bg` 底、active 白卡红字圆角 8；右顶部类目 banner 图+子类目 3 列卡片（圆角 8 图+名）；**清除 litemall 字标与旧 slogan** | Sidebar/TreeSelect 思路自绘 |
| 搜索 | 热搜胶囊标签、历史可清空、结果复用商品卡；搜索框胶囊+品牌色焦点环 | Search/Tag |
| 详情 | **骨架屏三态**（加载/空/错）；画廊指示点品牌色；价格卡：现价 28 红粗+划线价灰间距 8+促销标签；规格/属性/运费圆角卡片组；详情图圆角 12+12 间距；底栏白磨砂壳内定位：左客服/购物车(badge)图标组+右「加入购物车」红描边胶囊+「立即购买」红实胶囊，**废渐变** | Skeleton/Swipe/ActionBar 思路 |
| 购物车 | 圆角商品卡+品牌色 stepper+失效商品灰置分区；全选/合计/结算底栏壳内对齐；空态插画+去逛逛 CTA | SwipeCell/Stepper/SubmitBar |
| 结算/支付 | 地址卡（定位图标+箭头）、券/积分 cell 圆角组、金额明细卡（行右对齐 tabular）、支付方式 radio 卡；支付结果页状态插画 | Cell/Radio/Icon |
| 我的 | 头部**同色系**品牌红→deep 渐变+白头像/昵称/设置；订单四宫格图标+badge；模块列表圆角卡片组；券列表票券化 | Cell/Icon/Badge |
| AI 导购 | 悬浮钮改品牌红实底白图标+阴影；对话弹层圆角 16+阴影：用户红气泡/助手白气泡、推荐商品卡复用首页商品卡、输入条胶囊+发送红钮；加载打字动效 | Popup/自绘气泡 |

### 3.2 admin（Element Plus 映射）

| 区域 | 重设计要点 |
|---|---|
| 登录 | 左品牌面板（品牌红底+logo+slogan+几何纹样）右白表单卡；验证码圆角 8 描边容器；错误提示行内红字+输入红描边；保留语言切换但收进表单卡右上 |
| 框架 | **浅色侧栏**：白底、`--text-2` 文字、active 白→`--brand-soft` 底+红字+左 3px 红条；navbar 白+面包屑+全局搜索+头像下拉；TagsView 胶囊标签（active 品牌浅底红字）；折叠 64px 图标轨 |
| 仪表盘 | KPI 4 卡（浅色图标底块+28 数字+环比 tag）+ **echarts5 趋势图**（近 30 日订单/销售额双轴）+ 待办卡（待发货/待售后/库存预警计数跳转）+ 最近订单表 + AI 导购统计入口卡 |
| 列表页通用 | 统一 PageHeader（题+描述+右主操作）；筛选白色圆角卡可折叠；表格容器圆角 12+斑马纹+hover 行+表头 `--bg`；空态插画+文案+操作；分页右对齐；批量操作浮底 bar |
| 表单页通用 | 分步/分组圆角卡（商品创建=基础/规格/详情/营销 4 组）；sticky 底部操作条（保存草稿/提交）；校验错误行内+滚动定位 |
| AI 会话/统计 | 会话列表+对话时间线气泡（复用 H5 气泡规范）；统计卡复用仪表盘 KPI 规范+推荐点击率漏斗图 |

---

## 四、工程落地（与 2D 迁移绑定）

1. **tokens 单源**：仓库根 `design/tokens.css`（CSS variables + 暗色映射），两端构建引入；
   Vant4 用 `ConfigProvider :theme-vars` 映射 token；Element Plus 用 CSS variables 模式
   （`--el-color-primary` 等）映射，**删除编译期全量 SCSS import**。
2. **覆写清零**：删除 `_vant-theme.scss` 全部类名 hack 与魔法数；scoped 样式一律 `var(--*)`；
   CI 加 stylelint  declaration-no-important + 禁硬编码色值（allow list 仅 tokens 文件）。
3. **组件迁移对照**：Vant2→4（GoodsAction→ActionBar、Card 重排、Skeleton/ConfigProvider 新增、
   Lazyload 指令 API 变化）；Element UI→Plus（icon 改 `@element-plus/icons-vue`、
   `el-submenu` 等命名/事件变化、datetime picker API）；echarts 4→5（按需 import）。
4. **三态与 a11y 组件**：`SkeletonBlock/EmptyState/ErrorState` 两端各一套；
   对比度 ≥4.5:1、focus-visible 环、点击目标 ≥44px、图片 aspect-ratio 占位防 CLS。
5. **验收清单**：每页改前后截屏对照入 `docs/design-shots/`；token 覆盖审计（grep 硬编码色=0）；
   Lighthouse a11y ≥90；480 壳/1280 admin 双断点截屏；暗色开关 demo（可选）。
6. **分期**（每期独立可验收，后端零改动）：
   - 2D-1：tokens + H5 壳/登录/首页（含 banner 修复）
   - 2D-2：H5 分类/搜索/详情（含骨架三态）
   - 2D-3：H5 购物车/结算/我的 + AgentChat 视觉
   - 2D-4：admin 登录/框架/仪表盘（echarts5）
   - 2D-5：admin 列表/表单通用规范 + AI 会话/统计页
7. **迁移前 quick wins（可选预项，低风险纯 CSS/模板）**：banner img cover、详情加载守卫防 ¥NaN、
   清除 litemall/leomall-vue 残留文案、swiper 点色、购买条与 AI 钮去渐变。
   是否先做由用户决定；不做则随 2D-1/2D-2 一并解决。

## 五、风险与取舍

- Vant4/Element Plus 主题机制与 v2 完全不同：旧覆写**不可搬运**，本方案按新机制重建，工作量已计入分期。
- 管理端主色由蓝改品牌红：牺牲一点"后台蓝"惯例换品牌统一；若偏好冷静后台，可仅侧栏/按钮用
  `--info` 深灰蓝而保留红为强调色——token 化后切换成本为零（默认推荐全品牌红）。
- 首页演示素材（日文 banner/MUJI 文案）属内容运营问题：本方案只规范容器与比例，素材替换另行提供。
- 纯表现层改动，接口/路由/状态管理不变；2C 的 chat 契约（sessionId/reply/recommendations）保持。

---

## 六、决策记录与 quick wins 执行记录（2026-11-24）

**用户决策**：① 管理端主色统一品牌红（#D93A3A），不保留 Element 蓝；② 迁移前先执行 quick wins。

**已执行（本地 dev 双端热更新后逐页截屏验收，见 design-shots/after-*）**：

| # | 项 | 改动文件 | 验收 |
|---|---|---|---|
| 1 | 首页 banner 铺满 | `h5/src/views/home/tabbar-home.vue`（img width:100% + height:230px + object-fit:cover） | after-home：无右侧灰底 |
| 2 | 详情加载守卫 | `h5/src/views/items/detail/index.vue`（`v-if="goods.info.id"` 包裹内容 + van-loading 占位） | after3-detail-guard：错误路径红 spinner，无 ¥NaN |
| 3 | 残留清理 | `tabbar-catalog.vue`（删 class_tree_all 字标图、.box 改流式）、`login/login.vue`（leomall-vue→LeoMall） | after-catalog / after-login |
| 4 | swiper 指示点品牌红 | `_vant-theme.scss` | after3-detail：红点 |
| 5 | 购买条/AI 钮去渐变 | `_vant-theme.scss`（warning 白底描边 / danger 扁平红）、`AgentChat/index.vue`（$agent-color→$red、fab 扁平） | after3-detail / after-home |
| 6 | 管理端主色统一 | `admin/src/styles/element-variables.scss`（$--color-primary→#D93A3A） | after-admin-login：红按钮 |
| 7 | 品牌色 token | `_var.scss` $red→#D93A3A（全站 $red 引用随之统一） | 各页价格/按钮同色 |

**验收中发现并顺带修复的三个缺陷**：
- **懒加载占位图是 litemall logo**：`assets/images/goods_default.png` 被 Lazyload loading/error 与 AgentChat 兜底引用，画廊未加载 slide 露出上游字标 → 重绘为中性浅灰底 + 淡灰 LeoMall 字标占位图（after2-detail-loading）。
- **swiper 覆写优先级不足**：单类选择器与 vant 组件 css 同特异性且加载序在后 → 改 `.van-swipe__indicators .van-swipe__indicator--active`（0,2,0）稳定胜出。
- **详情路由参数切换不刷新**：vue-router 复用组件、created 不重跑，hash 直切商品 id 展示旧数据 → 加 `watch: itemId` 重置 goods 并重载（after3-detail：1006007 正确显示 ¥459）。

**说明**：本地 API <100ms，正常路径加载窗口短于截屏延迟，守卫视觉验收采用错误路径（spinner 常驻）；真实弱网下正常路径同样受守卫保护。