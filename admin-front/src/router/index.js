import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

/**
 * 路由与后端导航菜单 id 一一对应（a6 ~ a10），
 * 菜单数据由 GET /api/meta/nav 下发，保证前后端单一数据源。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true, title: '管理员登录' },
    },
    {
      path: '/',
      redirect: '/cluster',
    },
    {
      path: '/cluster',
      name: 'a6',
      component: () => import('@/views/ClusterOpsView.vue'),
      meta: { title: '集群态势感知与推演监控大盘' },
    },
    {
      path: '/models',
      name: 'a7',
      component: () => import('@/views/ModelHubView.vue'),
      meta: { title: 'AI 模型生命周期与热更中心' },
    },
    {
      path: '/moderation',
      name: 'a8',
      component: () => import('@/views/ModerationView.vue'),
      meta: { title: '语种术语库审核与 UGC 风控中台' },
    },
    {
      path: '/ads',
      name: 'a9',
      component: () => import('@/views/AdSchedulerView.vue'),
      meta: { title: '全网广告位排期与调度引擎' },
    },
    {
      path: '/security',
      name: 'a10',
      component: () => import('@/views/SecurityView.vue'),
      meta: { title: '安全风控、设备审计与 RBAC 权限' },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/cluster',
    },
  ],
})

// 登录守卫：未登录访问受保护页面 → 跳登录页；已登录访问登录页 → 跳首页
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    return auth.isLoggedIn ? '/' : true
  }
  if (!auth.isLoggedIn) {
    return { path: '/login' }
  }
  // token 存在但尚未经后端校验（如刷新后首次导航）：
  // 先用 /auth/me 校验，失效令牌立即清除并踢回登录页，避免"看得见框架、加载不出数据"的中间态
  if (!auth.user) {
    await auth.restore()
    if (!auth.isLoggedIn) {
      return { path: '/login' }
    }
  }
  return true
})

router.afterEach((to) => {
  document.title = `${to.meta.title || '平台管理后台'} | 视界译 VisionTrans`
  // 顶栏搜索是"本页数据"过滤：换页即清空，避免上一页关键字把新页面过滤成空表
  useAppStore().setKeyword('')
})

export default router
