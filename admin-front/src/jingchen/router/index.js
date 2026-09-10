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
  {
    path: '/merchant/plans',
    name: 'merchantPlans',
    component: () => import('@/jingchen/views/MerchantPlansView.vue'),
    meta: { title: '投放计划管理', roles: [MERCHANT_ROLE] },
  },
  {
    path: '/merchant/overview',
    name: 'merchantOverview',
    component: () => import('@/jingchen/views/MerchantOverviewView.vue'),
    meta: { title: '经营概览', roles: [MERCHANT_ROLE] },
  },
  {
    path: '/merchant/orders',
    name: 'merchantOrders',
    component: () => import('@/jingchen/views/MerchantOrdersView.vue'),
    meta: { title: '订单与结算', roles: [MERCHANT_ROLE] },
  },
  {
    path: '/merchant/materials',
    name: 'merchantMaterials',
    component: () => import('@/jingchen/views/MerchantMaterialsView.vue'),
    meta: { title: '素材管理', roles: [MERCHANT_ROLE] },
  },
  {
    path: '/merchant/videos',
    name: 'merchantVideos',
    component: () => import('@/jingchen/views/MerchantVideosView.vue'),
    meta: { title: '视频接入', roles: [MERCHANT_ROLE] },
  },
  {
    path: '/merchant/promo',
    name: 'merchantPromo',
    component: () => import('@/jingchen/views/MerchantPromoView.vue'),
    meta: { title: '推广与销售报表', roles: [MERCHANT_ROLE] },
  },
  {
    path: '/merchant/goods',
    name: 'merchantGoods',
    component: () => import('@/jingchen/views/MerchantGoodsView.vue'),
    meta: { title: '商品管理', roles: [MERCHANT_ROLE] },
  },
  {
    path: '/merchant/funds',
    name: 'merchantFunds',
    component: () => import('@/jingchen/views/MerchantFundsView.vue'),
    meta: { title: '账户与资金', roles: [MERCHANT_ROLE] },
  },
  {
    path: '/merchant/onboard',
    name: 'merchantOnboard',
    component: () => import('@/jingchen/views/MerchantOnboardView.vue'),
    meta: { title: '入驻管理', roles: [MERCHANT_ROLE] },
  },
]

export default merchantRoutes
