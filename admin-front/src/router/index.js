import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { MERCHANT_ROLE } from '@/jingchen/config'
// 模块路由由各模块自行维护后在此注册（商户模块：src/jingchen/router）
import { merchantRoutes } from '@/jingchen/router'

/** 后台管理角色：可访问 a6 ~ a11 全部页面（具体可见范围以后端下发的菜单为准） */
const ADMIN_ROLES = ['SUPER_ADMIN', 'OPERATIONS', 'AUDITOR']

/**
 * 按角色返回登录后的落地页：
 * 商户进商户工作台；管理员进「后端按角色下发的菜单」中的第一项，
 * 菜单尚未加载时回退到 /cluster。
 */
function homeFor(roleCode) {
  if (roleCode === MERCHANT_ROLE) return '/merchant'
  const app = useAppStore()
  const first = app.nav.groups?.find((g) => g.items?.length)?.items[0]
  if (!first) return '/cluster'
  try {
    return router.resolve({ name: first.id }).path
  } catch {
    return '/cluster'
  }
}

/** 是否为后台管理端页面（商户路由的 meta.roles 只含 MERCHANT） */
function isAdminPage(route) {
  return Array.isArray(route.meta.roles) && !route.meta.roles.includes(MERCHANT_ROLE)
}

/**
 * 路由与后端导航菜单 id 一一对应（a6 ~ a10），
 * 菜单数据由 GET /api/meta/nav 下发，保证前后端单一数据源。
 *
 * meta.roles 声明该页面允许访问的角色，由下面的守卫统一强制校验，
 * 实现「商户看不到管理页、管理员进不了商户页」的双向隔离。
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
    // 商户模块路由（角色隔离见 meta.roles）
    ...merchantRoutes,
    {
      path: '/cluster',
      name: 'a6',
      component: () => import('@/views/ClusterOpsView.vue'),
      meta: { title: '集群态势感知与推演监控大盘', roles: ADMIN_ROLES },
    },
    {
      path: '/models',
      name: 'a7',
      component: () => import('@/views/ModelHubView.vue'),
      meta: { title: 'AI 模型生命周期与热更中心', roles: ADMIN_ROLES },
    },
    {
      path: '/moderation',
      name: 'a8',
      component: () => import('@/views/ModerationView.vue'),
      meta: { title: '语种术语库审核与 UGC 风控中台', roles: ADMIN_ROLES },
    },
    {
      path: '/ads',
      name: 'a9',
      component: () => import('@/views/AdSchedulerView.vue'),
      meta: { title: '全网广告位排期与调度引擎', roles: ADMIN_ROLES },
    },
    {
      path: '/security',
      name: 'a10',
      component: () => import('@/views/SecurityView.vue'),
      meta: { title: '安全风控、设备审计与 RBAC 权限', roles: ADMIN_ROLES },
    },
    {
      path: '/finance',
      name: 'a11',
      component: () => import('@/views/FinanceOrderView.vue'),
      meta: { title: '财务订单与商户结算中心', roles: ADMIN_ROLES },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/cluster',
    },
  ],
})

// 登录守卫：未登录访问受保护页面 → 跳登录页；已登录访问登录页 → 按角色送回各自首页
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    // 登录页始终放行：支持从工作台「返回」到登录页（页面内提供「返回工作台」入口），
    // 也支持在免验证码宽限期内直接重新登录或切换身份
    return true
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
  // 根路径按角色分发
  if (to.path === '/') {
    return { path: homeFor(auth.roleCode) }
  }
  // 角色与页面不匹配：强制回到该角色的首页（前端兜底，后端另有 @RequireRole 拦截接口）
  const allowed = to.meta.roles
  if (allowed && allowed.length && !allowed.includes(auth.roleCode)) {
    const home = homeFor(auth.roleCode)
    // 兜底页自身不满足该角色时（未知角色 / roleCode 为空）必须跳出，
    // 否则会持续重定向到同一路径，触发路由死循环（无限重定向）导致白屏
    if (home === to.path) {
      await auth.logout()
      return { path: '/login' }
    }
    return { path: home }
  }
  // 菜单权限兜底：后台页面必须出现在「当前角色可见的菜单」里。
  // 菜单由后端 GET /api/meta/nav 按角色过滤下发，前端不再硬编码可见范围。
  if (isAdminPage(to)) {
    const app = useAppStore()
    if (!app.nav.groups?.length) {
      await app.loadMeta()
    }
    if (app.nav.groups?.length && !app.navContains(String(to.name))) {
      return { path: homeFor(auth.roleCode) }
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
