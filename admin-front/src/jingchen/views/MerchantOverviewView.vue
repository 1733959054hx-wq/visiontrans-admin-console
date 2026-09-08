<script setup>
import { computed, onMounted } from 'vue'

import PageHeader from '@/components/PageHeader.vue'
import { useMerchantStore } from '@/jingchen/stores/merchant'

/**
 * 商户经营概览（jingchen 模块业务页）。
 *
 * 指标卡 + 近 7 日「消耗 / GMV」双系列柱状趋势 + 订单摘要；
 * 数据来自 /api/merchant/overview（@RequireRole(MERCHANT)）。
 */
const store = useMerchantStore()

const data = computed(() => store.overview)

const yuan = (n) => `¥ ${Number(n || 0).toLocaleString('zh-CN')}`
const wan = (n) => {
  const v = Number(n || 0)
  return v >= 10000 ? `${(v / 10000).toFixed(1)}万` : String(v)
}

/** 把后端指标卡(数值型)转为展示结构 */
const kpiCards = computed(() => {
  const k = data.value?.kpis || []
  return k.map((item, i) => ({
    label: item.label,
    value: i === 0 ? wan(item.value) : yuan(item.value),
    note: item.delta,
    icon: ['fa-eye', 'fa-coins', 'fa-sack-dollar', 'fa-receipt'][i] || 'fa-chart-line',
    c1: ['#1E3A8A', '#2563EB', '#0EA5E9', '#F59E0B'][i],
    c2: ['#2563EB', '#0EA5E9', '#22D3EE', '#FB923C'][i],
  }))
})

/** 柱状趋势:消耗(蓝) / GMV(琥珀),以两序列最大值归一 */
const trend = computed(() => {
  const d = data.value
  if (!d) return { max: 0, items: [] }
  const consume = d.consume || []
  const gmv = d.gmv || []
  const max = Math.max(1, ...consume, ...gmv)
  return {
    max,
    items: (d.days || []).map((day, i) => ({
      day,
      consume: consume[i] || 0,
      gmv: gmv[i] || 0,
      ch: Math.round(((consume[i] || 0) / max) * 100),
      gh: Math.round(((gmv[i] || 0) / max) * 100),
    })),
  }
})

onMounted(() => {
  if (!store.overview) store.loadOverview()
})
</script>

<template>
  <div class="p-5">
    <PageHeader title="经营概览" desc="近 7 日曝光、广告消耗与成交 GMV 的实时汇总">
      <template #actions>
        <button class="btn btn-ghost btn-sm" @click="store.loadOverview()">
          <i class="fa-solid fa-rotate"></i>刷新
        </button>
      </template>
    </PageHeader>

    <template v-if="data">
      <!-- 指标卡 -->
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div v-for="kpi in kpiCards" :key="kpi.label" class="card anim relative overflow-hidden p-4">
          <div
            class="absolute -right-8 -top-8 h-32 w-32 rounded-full"
            :style="`background:radial-gradient(circle,rgba(37,99,235,0.14),transparent 70%)`"
          ></div>
          <div class="flex items-start justify-between gap-2">
            <div class="min-w-0">
              <div class="text-[12px] text-sub">{{ kpi.label }}</div>
              <div class="num mt-1.5 text-[24px] font-bold leading-tight text-navy">{{ kpi.value }}</div>
              <div v-if="kpi.note" class="mt-1 text-[11px] text-sub">{{ kpi.note }}</div>
            </div>
            <div
              class="grid h-10 w-10 flex-none place-items-center rounded-xl text-[15px] text-white"
              :style="`background:linear-gradient(135deg,${kpi.c1},${kpi.c2})`"
            >
              <i class="fa-solid" :class="kpi.icon"></i>
            </div>
          </div>
        </div>
      </div>

      <!-- 近 7 日 消耗/GMV 趋势 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">近 7 日消耗与成交趋势</div>
            <div class="card-s">蓝色为广告消耗(元) · 琥珀为成交 GMV(元)</div>
          </div>
        </div>
        <div class="card-b">
          <div v-if="store.overviewLoading" class="flex h-48 items-center justify-center text-[12.5px] text-sub">
            <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载趋势…
          </div>
          <div v-else class="flex h-48 items-end gap-2">
            <div v-for="t in trend.items" :key="t.day" class="flex min-w-0 flex-1 flex-col items-center gap-1.5">
              <div class="flex w-full items-end justify-center gap-1" style="height: 132px">
                <div
                  class="w-2.5 rounded-t-sm"
                  style="background: linear-gradient(180deg, #2563eb, #0ea5e9)"
                  :style="`height:${t.ch}%`"
                  :title="`${t.day} 消耗 ${yuan(t.consume)}`"
                ></div>
                <div
                  class="w-2.5 rounded-t-sm"
                  style="background: linear-gradient(180deg, #f59e0b, #fbbf24)"
                  :style="`height:${t.gh}%`"
                  :title="`${t.day} GMV ${yuan(t.gmv)}`"
                ></div>
              </div>
              <div class="num text-[10px] text-sub">{{ t.day.slice(5) }}</div>
            </div>
          </div>
          <div class="mt-3 flex items-center gap-4 text-[11px] text-sub">
            <span><i class="mr-1 inline-block h-2 w-2 rounded-sm" style="background: #2563eb"></i>广告消耗</span>
            <span><i class="mr-1 inline-block h-2 w-2 rounded-sm" style="background: #f59e0b"></i>成交 GMV</span>
            <span class="ml-auto num">合计消耗 {{ yuan(trend.items.reduce((s, t) => s + t.consume, 0)) }}</span>
          </div>
        </div>
      </div>

      <!-- 订单摘要 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">订单摘要</div>
            <div class="card-s">渠道成交与结算进度一览,完整列表见「订单与结算」模块</div>
          </div>
        </div>
        <div class="card-b grid grid-cols-2 gap-3 md:grid-cols-4">
          <div class="rounded-lg border border-line p-3">
            <div class="text-[11px] text-sub">订单总数</div>
            <div class="num mt-1 text-[20px] font-bold text-navy">{{ data.orderSummary.total }}</div>
          </div>
          <div class="rounded-lg border border-line p-3">
            <div class="text-[11px] text-sub">已结算</div>
            <div class="num mt-1 text-[20px] font-bold text-emerald-600">{{ data.orderSummary.settled }}</div>
          </div>
          <div class="rounded-lg border border-amber-100 bg-amber-50/50 p-3">
            <div class="text-[11px] text-sub">待结算</div>
            <div class="num mt-1 text-[20px] font-bold text-amber-600">{{ data.orderSummary.pending }}</div>
          </div>
          <div class="rounded-lg border border-rose-100 bg-rose-50/50 p-3">
            <div class="text-[11px] text-sub">退款中</div>
            <div class="num mt-1 text-[20px] font-bold text-rose-500">{{ data.orderSummary.refunding }}</div>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="grid h-64 place-items-center text-[13px] text-sub">
      <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载经营概览…
    </div>
  </div>
</template>
