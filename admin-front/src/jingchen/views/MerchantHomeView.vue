<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import MerchantWelcomeCard from '@/jingchen/components/MerchantWelcomeCard.vue'
import { useMerchantStore } from '@/jingchen/stores/merchant'

/**
 * 商户工作台首页（jingchen 模块）。
 *
 * 三张业务卡全部为真实模块:经营概览 / 投放计划管理 / 订单与结算,
 * 每张卡展示对应模块的实时摘要,点击进入管理页。
 */
const store = useMerchantStore()
const router = useRouter()

onMounted(() => {
  if (!store.home) store.loadHome()
  if (!store.plans) store.loadPlans()
  if (!store.overview) store.loadOverview()
  if (!store.orders) store.loadOrders()
  if (!store.materials) store.loadMaterials()
  if (!store.videos) store.loadVideos()
  if (!store.channels) store.loadChannels()
  if (!store.goods) store.loadGoods()
  if (!store.funds) store.loadFunds()
  if (!store.onboard) store.loadOnboard()
})

const wan = (n) => {
  const v = Number(n || 0)
  return v >= 10000 ? `${(v / 10000).toFixed(1)}万` : String(v)
}

/** 模块卡片:每个都带真实摘要与入口 */
const modules = computed(() => {
  const plans = store.plans || []
  const orders = store.orders || []
  const materials = store.materials || []
  const videos = store.videos || []
  const channels = store.channels || []
  const goods = store.goods || []
  const funds = store.funds
  const ov = store.overview
  const planBudget = plans.reduce((s, p) => s + (p.budget || 0), 0)
  const planUsed = plans.reduce((s, p) => s + (p.used || 0), 0)
  const pending = orders.filter((o) => o.status === '待结算').length
  const sumMoney = (list, k, st) => list.filter((o) => !st || o.status === st).reduce((s, o) => s + (o[k] || 0), 0)
  const playSum = videos.reduce((s, v) => s + (v.plays || 0), 0)
  const dealSum = channels.reduce((s, c) => s + (c.dealCount || 0), 0)
  const goodsOn = goods.filter((g) => g.status === '在售').length
  const onboard = store.onboard
  const onboardState = !onboard ? '未提交' : onboard.status === '已通过' ? '已通过' : onboard.status === '已驳回' ? '已驳回' : onboard.contractStatus === '已签署' ? '审核中' : '待签署'
  return [
    {
      key: 'onboard',
      title: '入驻管理',
      icon: 'fa-building-shield',
      grad: 'linear-gradient(135deg,#475569,#94A3B8)',
      desc: '资质提交 / 合同签署 / 平台审核',
      stats: [
        { label: '状态', value: onboardState },
        { label: '申请单', value: onboard ? onboard.applyNo : '—' },
        { label: '合同', value: onboard ? onboard.contractStatus : '未签署' },
      ],
      to: () => router.push({ name: 'merchantOnboard' }),
    },
    {
      key: 'overview',
      title: '经营概览',
      icon: 'fa-chart-line',
      grad: 'linear-gradient(135deg,#06B6D4,#22D3EE)',
      desc: '近 7 日曝光 / 消耗 / 成交汇总',
      stats: [
        { label: '曝光', value: wan(ov?.kpis?.[0]?.value) },
        { label: '消耗', value: `¥${wan(ov?.kpis?.[1]?.value)}` },
        { label: 'GMV', value: `¥${wan(ov?.kpis?.[2]?.value)}` },
      ],
      to: () => router.push({ name: 'merchantOverview' }),
    },
    {
      key: 'plans',
      title: '投放计划管理',
      icon: 'fa-bullhorn',
      grad: 'linear-gradient(135deg,#0F766E,#0D9488)',
      desc: '投放计划增删改查与暂停恢复',
      stats: [
        { label: '计划', value: String(plans.length) },
        { label: '日预算', value: `¥${wan(planBudget)}` },
        { label: '已消耗', value: `¥${wan(planUsed)}` },
      ],
      to: () => router.push({ name: 'merchantPlans' }),
    },
    {
      key: 'goods',
      title: '商品管理',
      icon: 'fa-box-open',
      grad: 'linear-gradient(135deg,#7C3AED,#A78BFA)',
      desc: '商品上架 / 编辑 / 下架与折扣定价',
      stats: [
        { label: '商品', value: `${goods.length} 个` },
        { label: '在售', value: `${goodsOn} 个` },
        { label: '销量', value: `${Number(goods.reduce((s, g) => s + (g.salesCount || 0), 0)).toLocaleString('zh-CN')} 份` },
      ],
      to: () => router.push({ name: 'merchantGoods' }),
    },
    {
      key: 'orders',
      title: '订单与结算',
      icon: 'fa-file-invoice-dollar',
      grad: 'linear-gradient(135deg,#F59E0B,#FB923C)',
      desc: '渠道成交与待结算流转',
      stats: [
        { label: '成交总额', value: `¥${wan(sumMoney(orders, 'amount'))}` },
        { label: '待结算', value: `${pending} 笔` },
        { label: '退款中', value: `${orders.filter((o) => o.status === '退款中').length} 笔` },
      ],
      to: () => router.push({ name: 'merchantOrders' }),
    },
    {
      key: 'funds',
      title: '账户与资金',
      icon: 'fa-wallet',
      grad: 'linear-gradient(135deg,#10B981,#34D399)',
      desc: '余额 / 充值提现 / 资金流水',
      stats: [
        { label: '可用余额', value: `¥${wan(funds?.balance || 0)}` },
        { label: '累计收入', value: `¥${wan(funds?.income || 0)}` },
        { label: '累计支出', value: `¥${wan(funds?.expense || 0)}` },
      ],
      to: () => router.push({ name: 'merchantFunds' }),
    },
    {
      key: 'materials',
      title: '素材管理',
      icon: 'fa-photo-film',
      grad: 'linear-gradient(135deg,#7C3AED,#A78BFA)',
      desc: '素材库与 A/B 分流测试',
      stats: [
        { label: '素材', value: String(materials.length) },
        { label: '使用中', value: `${materials.filter((m) => m.status === '使用中').length} 个` },
        { label: 'B 组占比', value: store.ab ? `${store.ab.ratioB}%` : '—' },
      ],
      to: () => router.push({ name: 'merchantMaterials' }),
    },
    {
      key: 'videos',
      title: '视频接入',
      icon: 'fa-film',
      grad: 'linear-gradient(135deg,#0891B2,#22D3EE)',
      desc: '视频档案 / 字幕 / 播放数据',
      stats: [
        { label: '视频', value: `${videos.length} 部` },
        { label: '累计播放', value: wan(playSum) },
        { label: '处理中', value: `${videos.filter((v) => v.status === '转码中').length} 部` },
      ],
      to: () => router.push({ name: 'merchantVideos' }),
    },
    {
      key: 'promo',
      title: '推广与销售',
      icon: 'fa-share-nodes',
      grad: 'linear-gradient(135deg,#DB2777,#F472B6)',
      desc: '渠道链接与销售报表',
      stats: [
        { label: '渠道', value: `${channels.length} 个` },
        { label: '渠道成交', value: `${dealSum} 笔` },
        { label: '报表', value: '随时查看' },
      ],
      to: () => router.push({ name: 'merchantPromo' }),
    },
  ]
})
</script>

<template>
  <div class="p-5">
    <!-- 欢迎模块 -->
    <MerchantWelcomeCard :home="store.home" :loading="store.loading" />

    <!-- 业务模块:入驻 + 九大业务模块入口 -->
    <div class="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
      <div v-for="mod in modules" :key="mod.key" class="card anim overflow-hidden">
        <div class="card-h">
          <div class="flex items-center gap-3">
            <span
              class="grid h-10 w-10 flex-none place-items-center rounded-xl text-white"
              :style="`background:${mod.grad}`"
            >
              <i class="fa-solid" :class="mod.icon"></i>
            </span>
            <div>
              <div class="card-t">{{ mod.title }}</div>
              <div class="card-s">{{ mod.desc }}</div>
            </div>
          </div>
        </div>

        <div class="px-4 pb-2">
          <div
            v-if="(mod.key === 'overview' && store.overviewLoading) || (mod.key === 'orders' && store.ordersLoading)"
            class="py-5 text-center text-[12px] text-sub"
          >
            <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载中…
          </div>
          <div v-else class="grid grid-cols-3 gap-2">
            <div
              v-for="stat in mod.stats"
              :key="stat.label"
              class="rounded-lg bg-[#F8FAFC] px-2 py-2.5 text-center"
            >
              <div class="num text-[13px] font-bold leading-none text-navy">{{ stat.value }}</div>
              <div class="mt-1 text-[10.5px] text-sub">{{ stat.label }}</div>
            </div>
          </div>
        </div>
        <div class="card-b pt-1">
          <button class="btn btn-primary btn-sm w-full" @click="mod.to()">
            <i class="fa-solid fa-arrow-right"></i>进入{{ mod.title.replace('管理', '') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
