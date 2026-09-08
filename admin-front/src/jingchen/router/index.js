import { MERCHANT_ROLE } from '@/jingchen/config'

/**
 * 商户模块路由（模块自有，由主工程 src/router/index.js 统一注册）。
 *
 * meta.roles 声明允许访问的角色，由主工程守卫统一强制校验。
 */
export const merchantRoutes = [
  {
    path: '/merchant',
    name: 'merchant',
    component: () => import('@/jingchen/views/MerchantHomeView.vue'),
    meta: { title: '商户工作台', roles: [MERCHANT_ROLE] },
  },
]

export default merchantRoutes
