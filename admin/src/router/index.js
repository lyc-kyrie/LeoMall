import { createRouter, createWebHashHistory } from 'vue-router'

/* Layout */
import Layout from '@/views/layout/Layout'

/** note: Submenu only appear when children.length>=1
 *  detail see  https://panjiachen.github.io/vue-element-admin-site/guide/essentials/router-and-nav.html
 **/

/**
* hidden: true                   if `hidden:true` will not show in the sidebar(default is false)
* alwaysShow: true               if set true, will always show the root menu, whatever its child routes length
*                                if not set alwaysShow, only more than one route under the children
*                                it will becomes nested mode, otherwise not show the root menu
* redirect: noredirect           if `redirect:noredirect` will no redirect in the breadcrumb
* name:'router-name'             the name is used by <keep-alive> (must set!!!)
* meta : {
    perms: ['GET /aaa','POST /bbb']     will control the page perms (you can set multiple perms)
    title: 'title'               the name show in submenu and breadcrumb (recommend set)
    icon: 'svg-name'             the icon show in the sidebar,
    noCache: true                if true ,the page will no be cached(default is false)
  }
**/
export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login/index'),
    hidden: true
  },
  {
    path: '/auth-redirect',
    component: () => import('@/views/login/authredirect'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/errorPage/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/errorPage/401'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: 'dashboard',
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/dashboard/index'),
        name: 'Dashboard',
        meta: { title: 'app.menu.dashboard', icon: 'dashboard', affix: true }
      }
    ]
  }
]

export const asyncRoutes = [
  {
    path: '/user',
    component: Layout,
    redirect: 'noredirect',
    alwaysShow: true,
    name: 'userManage',
    meta: {
      title: 'app.menu.user',
      icon: 'chart'
    },
    children: [
      {
        path: 'user',
        component: () => import('@/views/user/user'),
        name: 'user',
        meta: {
          perms: ['GET /leomall/admin/user/list'],
          title: 'app.menu.user_user',
          noCache: true
        }
      },
      {
        path: 'address',
        component: () => import('@/views/user/address'),
        name: 'address',
        meta: {
          perms: ['GET /leomall/admin/address/list'],
          title: 'app.menu.user_address',
          noCache: true
        }
      },
      {
        path: 'collect',
        component: () => import('@/views/user/collect'),
        name: 'collect',
        meta: {
          perms: ['GET /leomall/admin/collect/list'],
          title: 'app.menu.user_collect',
          noCache: true
        }
      },
      {
        path: 'footprint',
        component: () => import('@/views/user/footprint'),
        name: 'footprint',
        meta: {
          perms: ['GET /leomall/admin/footprint/list'],
          title: 'app.menu.user_footprint',
          noCache: true
        }
      },
      {
        path: 'history',
        component: () => import('@/views/user/history'),
        name: 'history',
        meta: {
          perms: ['GET /leomall/admin/history/list'],
          title: 'app.menu.user_history',
          noCache: true
        }
      },
      {
        path: 'feedback',
        component: () => import('@/views/user/feedback'),
        name: 'feedback',
        meta: {
          perms: ['GET /leomall/admin/feedback/list'],
          title: 'app.menu.user_feedback',
          noCache: true
        }
      }
    ]
  },

  {
    path: '/mall',
    component: Layout,
    redirect: 'noredirect',
    alwaysShow: true,
    name: 'mallManage',
    meta: {
      title: 'app.menu.mall',
      icon: 'chart'
    },
    children: [
      {
        path: 'region',
        component: () => import('@/views/mall/region'),
        name: 'region',
        meta: {
          title: 'app.menu.mall_region',
          noCache: true
        }
      },
      {
        path: 'brand',
        component: () => import('@/views/mall/brand'),
        name: 'brand',
        meta: {
          perms: ['GET /leomall/admin/brand/list', 'POST /leomall/admin/brand/create', 'GET /leomall/admin/brand/read', 'POST /leomall/admin/brand/update', 'POST /leomall/admin/brand/delete'],
          title: 'app.menu.mall_brand',
          noCache: true
        }
      },
      {
        path: 'category',
        component: () => import('@/views/mall/category'),
        name: 'category',
        meta: {
          perms: ['GET /leomall/admin/category/list', 'POST /leomall/admin/category/create', 'GET /leomall/admin/category/read', 'POST /leomall/admin/category/update', 'POST /leomall/admin/category/delete'],
          title: 'app.menu.mall_category',
          noCache: true
        }
      },
      {
        path: 'order',
        component: () => import('@/views/mall/order'),
        name: 'order',
        meta: {
          perms: ['GET /leomall/admin/order/list', 'GET /leomall/admin/order/detail', 'POST /leomall/admin/order/ship', 'POST /leomall/admin/order/refund', 'POST /leomall/admin/order/delete', 'POST /leomall/admin/order/reply'],
          title: 'app.menu.mall_order',
          noCache: true
        }
      },
      {
        path: 'aftersale',
        component: () => import('@/views/mall/aftersale'),
        name: 'aftersale',
        meta: {
          perms: ['GET /leomall/admin/aftersale/list', 'GET /leomall/admin/aftersale/detail', 'POST /leomall/admin/order/receive', 'POST /leomall/admin/aftersale/complete', 'POST /leomall/admin/aftersale/reject'],
          title: 'app.menu.mall_aftersale',
          noCache: true
        }
      },
      {
        path: 'issue',
        component: () => import('@/views/mall/issue'),
        name: 'issue',
        meta: {
          perms: ['GET /leomall/admin/issue/list', 'POST /leomall/admin/issue/create', 'GET /leomall/admin/issue/read', 'POST /leomall/admin/issue/update', 'POST /leomall/admin/issue/delete'],
          title: 'app.menu.mall_issue',
          noCache: true
        }
      },
      {
        path: 'keyword',
        component: () => import('@/views/mall/keyword'),
        name: 'keyword',
        meta: {
          perms: ['GET /leomall/admin/keyword/list', 'POST /leomall/admin/keyword/create', 'GET /leomall/admin/keyword/read', 'POST /leomall/admin/keyword/update', 'POST /leomall/admin/keyword/delete'],
          title: 'app.menu.mall_keyword',
          noCache: true
        }
      }
    ]
  },

  {
    path: '/goods',
    component: Layout,
    redirect: 'noredirect',
    alwaysShow: true,
    name: 'goodsManage',
    meta: {
      title: 'app.menu.goods',
      icon: 'chart'
    },
    children: [
      {
        path: 'list',
        component: () => import('@/views/goods/list'),
        name: 'goodsList',
        meta: {
          perms: ['GET /leomall/admin/goods/list', 'POST /leomall/admin/goods/delete'],
          title: 'app.menu.goods_list',
          noCache: true
        }
      },
      {
        path: 'create',
        component: () => import('@/views/goods/create'),
        name: 'goodsCreate',
        meta: {
          perms: ['POST /leomall/admin/goods/create'],
          title: 'app.menu.goods_create',
          noCache: true
        }
      },
      {
        path: 'edit',
        component: () => import('@/views/goods/edit'),
        name: 'goodsEdit',
        meta: {
          perms: ['GET /leomall/admin/goods/detail', 'POST /leomall/admin/goods/update', 'POST /leomall/admin/goods/catAndBrand'],
          title: 'app.menu.goods_edit',
          noCache: true
        },
        hidden: true
      },
      {
        path: 'comment',
        component: () => import('@/views/goods/comment'),
        name: 'goodsComment',
        meta: {
          perms: ['GET /leomall/admin/comment/list', 'POST /leomall/admin/comment/delete'],
          title: 'app.menu.goods_comment',
          noCache: true
        }
      }
    ]
  },
  {
    path: '/promotion',
    component: Layout,
    redirect: 'noredirect',
    alwaysShow: true,
    name: 'promotionManage',
    meta: {
      title: 'app.menu.promotion',
      icon: 'chart'
    },
    children: [
      {
        path: 'ad',
        component: () => import('@/views/promotion/ad'),
        name: 'ad',
        meta: {
          perms: ['GET /leomall/admin/ad/list', 'POST /leomall/admin/ad/create', 'GET /leomall/admin/ad/read', 'POST /leomall/admin/ad/update', 'POST /leomall/admin/ad/delete'],
          title: 'app.menu.promotion_ad',
          noCache: true
        }
      },
      {
        path: 'coupon',
        component: () => import('@/views/promotion/coupon'),
        name: 'coupon',
        meta: {
          perms: ['GET /leomall/admin/coupon/list', 'POST /leomall/admin/coupon/create', 'POST /leomall/admin/coupon/update', 'POST /leomall/admin/coupon/delete'],
          title: 'app.menu.promotion_coupon',
          noCache: true
        }
      },
      {
        path: 'couponDetail',
        component: () => import('@/views/promotion/couponDetail'),
        name: 'couponDetail',
        meta: {
          perms: ['GET /leomall/admin/coupon/list', 'GET /leomall/admin/coupon/listuser'],
          title: 'app.menu.promotion_coupon_detail',
          noCache: true
        },
        hidden: true
      },
      {
        path: 'topic',
        component: () => import('@/views/promotion/topic'),
        name: 'topic',
        meta: {
          perms: ['GET /leomall/admin/topic/list', 'POST /leomall/admin/topic/create', 'GET /leomall/admin/topic/read', 'POST /leomall/admin/topic/update', 'POST /leomall/admin/topic/delete'],
          title: 'app.menu.promotion_topic',
          noCache: true
        }
      },
      {
        path: 'topic-create',
        component: () => import('@/views/promotion/topicCreate'),
        name: 'topicCreate',
        meta: {
          perms: ['POST /leomall/admin/topic/create'],
          title: 'app.menu.promotion_topic_create',
          noCache: true
        },
        hidden: true
      },
      {
        path: 'topic-edit',
        component: () => import('@/views/promotion/topicEdit'),
        name: 'topicEdit',
        meta: {
          perms: ['GET /leomall/admin/topic/read', 'POST /leomall/admin/topic/update'],
          title: 'app.menu.promotion_topic_edit',
          noCache: true
        },
        hidden: true
      },
      {
        path: 'groupon-rule',
        component: () => import('@/views/promotion/grouponRule'),
        name: 'grouponRule',
        meta: {
          perms: ['GET /leomall/admin/groupon/list', 'POST /leomall/admin/groupon/create', 'POST /leomall/admin/groupon/update', 'POST /leomall/admin/groupon/delete'],
          title: 'app.menu.promotion_groupon_rule',
          noCache: true
        }
      },
      {
        path: 'groupon-activity',
        component: () => import('@/views/promotion/grouponActivity'),
        name: 'grouponActivity',
        meta: {
          perms: ['GET /leomall/admin/groupon/listRecord'],
          title: 'app.menu.promotion_groupon_activity',
          noCache: true
        }
      }
    ]
  },

  {
    path: '/sys',
    component: Layout,
    redirect: 'noredirect',
    alwaysShow: true,
    name: 'sysManage',
    meta: {
      title: 'app.menu.sys',
      icon: 'chart'
    },
    children: [
      {
        path: 'admin',
        component: () => import('@/views/sys/admin'),
        name: 'admin',
        meta: {
          perms: ['GET /leomall/admin/admin/list', 'POST /leomall/admin/admin/create', 'POST /leomall/admin/admin/update', 'POST /leomall/admin/admin/delete'],
          title: 'app.menu.sys_admin',
          noCache: true
        }
      },
      {
        path: 'notice',
        component: () => import('@/views/sys/notice'),
        name: 'sysNotice',
        meta: {
          perms: ['GET /leomall/admin/notice/list', 'POST /leomall/admin/notice/create', 'POST /leomall/admin/notice/update', 'POST /leomall/admin/notice/delete'],
          title: 'app.menu.sys_notice',
          noCache: true
        }
      },
      {
        path: 'log',
        component: () => import('@/views/sys/log'),
        name: 'log',
        meta: {
          perms: ['GET /leomall/admin/log/list'],
          title: 'app.menu.sys_log',
          noCache: true
        }
      },
      {
        path: 'role',
        component: () => import('@/views/sys/role'),
        name: 'role',
        meta: {
          perms: ['GET /leomall/admin/role/list', 'POST /leomall/admin/role/create', 'POST /leomall/admin/role/update', 'POST /leomall/admin/role/delete', 'GET /leomall/admin/role/permissions', 'POST /leomall/admin/role/permissions'],
          title: 'app.menu.sys_role',
          noCache: true
        }
      },
      {
        path: 'os',
        component: () => import('@/views/sys/os'),
        name: 'os',
        meta: {
          perms: ['GET /leomall/admin/storage/list', 'POST /leomall/admin/storage/create', 'POST /leomall/admin/storage/update', 'POST /leomall/admin/storage/delete'],
          title: 'app.menu.sys_os',
          noCache: true
        }
      }
    ]
  },

  {
    path: '/config',
    component: Layout,
    redirect: 'noredirect',
    alwaysShow: true,
    name: 'configManage',
    meta: {
      title: 'app.menu.config',
      icon: 'chart'
    },
    children: [
      {
        path: 'mall',
        component: () => import('@/views/config/mall'),
        name: 'configMall',
        meta: {
          perms: ['GET /leomall/admin/config/mall', 'POST /leomall/admin/config/mall'],
          title: 'app.menu.config_mall',
          noCache: true
        }
      },
      {
        path: 'express',
        component: () => import('@/views/config/express'),
        name: 'configExpress',
        meta: {
          perms: ['GET /leomall/admin/config/express', 'POST /leomall/admin/config/express'],
          title: 'app.menu.config_express',
          noCache: true
        }
      },
      {
        path: 'order',
        component: () => import('@/views/config/order'),
        name: 'configOrder',
        meta: {
          perms: ['GET /leomall/admin/config/order', 'POST /leomall/admin/config/order'],
          title: 'app.menu.config_order',
          noCache: true
        }
      },
      {
        path: 'wx',
        component: () => import('@/views/config/wx'),
        name: 'configWx',
        meta: {
          perms: ['GET /leomall/admin/config/wx', 'POST /leomall/admin/config/wx'],
          title: 'app.menu.config_wx',
          noCache: true
        }
      }
    ]
  },

  {
    path: '/stat',
    component: Layout,
    redirect: 'noredirect',
    alwaysShow: true,
    name: 'statManage',
    meta: {
      title: 'app.menu.stat',
      icon: 'chart'
    },
    children: [
      {
        path: 'user',
        component: () => import('@/views/stat/user'),
        name: 'statUser',
        meta: {
          perms: ['GET /leomall/admin/stat/user'],
          title: 'app.menu.stat_user',
          noCache: true
        }
      },
      {
        path: 'order',
        component: () => import('@/views/stat/order'),
        name: 'statOrder',
        meta: {
          perms: ['GET /leomall/admin/stat/order'],
          title: 'app.menu.stat_order',
          noCache: true
        }
      },
      {
        path: 'goods',
        component: () => import('@/views/stat/goods'),
        name: 'statGoods',
        meta: {
          perms: ['GET /leomall/admin/stat/goods'],
          title: 'app.menu.stat_goods',
          noCache: true
        }
      }
    ]
  },
  {
    // 阶段2D-4a：vue-router4 顶级 path 必须以 / 开头；子路由 URL 含 ':' 会被当参数解析抛错，
    // 外链改存 meta.link，由 SidebarItem 渲染 <a>（路由表本身不再注册 URL path）
    path: '/external-link',
    component: Layout,
    redirect: 'noredirect',
    alwaysShow: true,
    name: 'externalLink',
    meta: {
      title: 'app.menu.external_link',
      icon: 'link'
    },
    children: [
      {
        path: 'tencent-cos',
        meta: { title: 'app.menu.external_link_tencent_cos', icon: 'link', link: 'https://cloud.tencent.com/product/cos' }
      },
      {
        path: 'tencent-sms',
        meta: { title: 'app.menu.external_link_tencent_sms', icon: 'link', link: 'https://cloud.tencent.com/product/sms' }
      },
      {
        path: 'weixin-pay',
        meta: { title: 'app.menu.external_link_weixin_pay', icon: 'link', link: 'https://pay.weixin.qq.com/index.php/core/home/login' }
      },
      {
        path: 'weixin-mpkf',
        meta: { title: 'app.menu.external_link_weixin_mpkf', icon: 'link', link: 'https://mpkf.weixin.qq.com/' }
      },
      {
        path: 'alibaba-oss',
        meta: { title: 'app.menu.external_link_alibaba_oss', icon: 'link', link: 'https://www.alibabacloud.com/zh/product/oss' }
      },
      {
        path: 'qiniu-kodo',
        meta: { title: 'app.menu.external_link_qiniu_kodo', icon: 'link', link: 'https://www.qiniu.com/products/kodo' }
      },
      {
        path: 'kdniao-api',
        meta: { title: 'app.menu.external_link_kdniao_api', icon: 'link', link: 'http://www.kdniao.com/api-track' }
      }
    ]
  },
  {
    path: '/profile',
    component: Layout,
    redirect: 'noredirect',
    alwaysShow: true,
    name: 'profileManage',
    children: [
      {
        path: 'password',
        component: () => import('@/views/profile/password'),
        name: 'password',
        meta: { title: 'app.menu.profile_password', noCache: true }
      },
      {
        path: 'notice',
        component: () => import('@/views/profile/notice'),
        name: 'notice',
        meta: { title: 'app.menu.profile_notice', noCache: true }
      }
    ],
    hidden: true
  },

  {
    path: '/agent',
    component: Layout,
    redirect: 'noredirect',
    alwaysShow: true,
    name: 'agentManage',
    meta: {
      title: 'app.menu.agent',
      icon: 'dashboard'
    },
    children: [
      {
        path: 'sessions',
        component: () => import('@/views/agent/sessions'),
        name: 'agentSessions',
        meta: {
          perms: ['GET /leomall/admin/agent/sessions'],
          title: 'app.menu.agent_sessions',
          noCache: true
        }
      },
      {
        path: 'stats',
        component: () => import('@/views/agent/stats'),
        name: 'agentStats',
        meta: {
          perms: ['GET /leomall/admin/agent/stats'],
          title: 'app.menu.agent_stats',
          noCache: true
        }
      }
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/404', hidden: true }
]

const newInstance = () => createRouter({
  history: createWebHashHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: constantRoutes
})

const router = newInstance()

// 阶段2D-4a：vue-router4 无 matcher 替换 hack，改为按名移除动态路由
const constantNames = new Set(
  constantRoutes.flatMap(r => [r.name, ...(r.children || []).map(c => c.name)]).filter(Boolean)
)
export function resetRouter() {
  router.getRoutes().forEach(r => {
    if (r.name && !constantNames.has(r.name)) router.removeRoute(r.name)
  })
}

export default router
