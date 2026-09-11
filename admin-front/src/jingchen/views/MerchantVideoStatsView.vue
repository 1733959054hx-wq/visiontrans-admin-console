<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import PageHeader from '@/components/PageHeader.vue'
import MerchantChart from '@/jingchen/components/MerchantChart.vue'
import { useMerchantStore } from '@/jingchen/stores/merchant'

/**
 * 视频播放统计（jingchen 模块专用视图）。
 *
 * 数据来自 GET /api/merchant/videos/stats（merchant_video_daily 表聚合）:
 * 指标卡 + 播放趋势(折线面积) + 地域分布(饼图) + 单视频对比(横向柱状) + 明细表。
 */
const store = useMerchantStore()
const router = useRouter()

const data = computed(() => store.videoStats)
const wan = (n) => {
  const v = Number(n || 0)
  return v >= 10000 ? `${(v / 10000).toFixed(1)}万` : String(v)
}

const kpis = computed(() => {
  const d = data.value
  if (!d) return []
  return [
    { label: '近 14 日播放量', value: wan(d.totalPlays), note: `日均 ${wan(d.dailyPlays)} 次`, icon: 'fa-circle-play', grad: 'linear-gradient(135deg,#0891B2,#22D3EE)' },
    { label: '平均完播率', value: d.avgFinishRate == null ? '—' : `${d.avgFinishRate}%`, note: '按播放量加权', icon: 'fa-gauge-high', grad: 'linear-gradient(135deg,#0F766E,#0D9488)' },
    { label: '覆盖地域', value: `${(d.regions || []).length} 个`, note: '按授权区域统计', icon: 'fa-earth-asia', grad: 'linear-gradient(135deg,#7C3AED,#A78BFA)' },
    { label: '在播视频', value: `${(d.videos || []).length} 部`, note: `转码中 ${(d.videos || []).filter((v) => v.status === '转码中').length} 部`, icon: 'fa-film', grad: 'linear-gradient(135deg,#F59E0B,#FB923C)' },
  ]
})

/** 播放趋势:折线面积 + 完播率(右轴) */
const trendOption = computed(() => {
  const d = data.value
  if (!d) return {}
  const dates = (d.trend || []).map((t) => t.date.slice(5))
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['播放量', '完播率'], top: 0, textStyle: { fontSize: 11, color: '#64748B' } },
    grid: { left: 8, right: 8, top: 30, bottom: 4, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: dates, axisLine: { lineStyle: { color: '#E2E8F0' } }, axisLabel: { fontSize: 10, color: '#94A3B8' } },
    yAxis: [
      { type: 'value', name: '播放', nameTextStyle: { fontSize: 10 }, splitLine: { lineStyle: { color: '#F1F5F9' } }, axisLabel: { fontSize: 10, color: '#94A3B8' } },
      { type: 'value', name: '完播率%', max: 100, nameTextStyle: { fontSize: 10 }, splitLine: { show: false }, axisLabel: { fontSize: 10, color: '#94A3B8' } },
    ],
    series: [
      {
        name: '播放量', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
        data: (d.trend || []).map((t) => t.plays),
        lineStyle: { width: 2.5, color: '#0D9488' },
        itemStyle: { color: '#0D9488' },
        areaStyle: { color: 'rgba(13, 148, 136, 0.14)' },
      },
      {
        name: '完播率', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5, yAxisIndex: 1,
        data: (d.trend || []).map((t) => t.finishRate),
        lineStyle: { width: 2, color: '#F59E0B', type: 'dashed' },
        itemStyle: { color: '#F59E0B' },
        connectNulls: true,
      },
    ],
  }
})

/** 地域分布饼图 */
const regionOption = computed(() => {
  const colors = ['#0D9488', '#06B6D4', '#F59E0B', '#7C3AED', '#DB2777']
  const list = (data.value?.regions || []).map((r, i) => ({
    name: r.region, value: r.plays, itemStyle: { color: colors[i % colors.length] },
  }))
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} 次({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 11, color: '#64748B' } },
    series: [
      {
        type: 'pie', radius: ['44%', '68%'], center: ['50%', '44%'],
        label: { formatter: '{b}\n{d}%', fontSize: 10, color: '#475569' },
        labelLine: { length: 10, length2: 8 },
        data: list,
      },
    ],
  }
})

/** 单视频播放对比(横向柱状,取 TOP 6) */
const videoBarOption = computed(() => {
  const list = (data.value?.videos || []).slice(0, 6)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 24, top: 8, bottom: 4, containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#F1F5F9' } }, axisLabel: { fontSize: 10, color: '#94A3B8' } },
    yAxis: {
      type: 'category',
      data: list.map((v) => v.name.length > 18 ? `${v.name.slice(0, 17)}…` : v.name).reverse(),
      axisLabel: { fontSize: 10, color: '#64748B' },
      axisLine: { lineStyle: { color: '#E2E8F0' } },
    },
    series: [
      {
        type: 'bar', barWidth: 12, borderRadius: [0, 6, 6, 0],
        data: list.map((v) => v.plays).reverse(),
        itemStyle: { borderRadius: [0, 6, 6, 0], color: 'rgba(13, 148, 136, 0.85)' },
        label: { show: true, position: 'right', fontSize: 10, color: '#64748B' },
      },
    ],
  }
})

onMounted(() => {
  if (!store.videoStats) store.loadVideoStats()
})
</script>

<template>
  <div class="p-5">
    <PageHeader title="视频播放统计" desc="播放趋势 · 完播率 · 地域分布（近 14 天）">
      <template #actions>
        <button class="btn btn-ghost btn-sm" @click="router.push({ name: 'merchantVideos' })">
          <i class="fa-solid fa-arrow-left"></i>返回视频库
        </button>
        <button class="btn btn-ghost btn-sm" @click="store.loadVideoStats()">
          <i class="fa-solid fa-rotate"></i>刷新
        </button>
      </template>
    </PageHeader>

    <template v-if="data">
      <!-- 指标卡 -->
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div v-for="kpi in kpis" :key="kpi.label" class="card anim p-4">
          <div class="flex items-center gap-3">
            <span class="grid h-10 w-10 flex-none place-items-center rounded-xl text-[15px] text-white" :style="`background:${kpi.grad}`">
              <i class="fa-solid" :class="kpi.icon"></i>
            </span>
            <div>
              <div class="text-[11px] text-sub">{{ kpi.label }}</div>
              <div class="num text-[20px] font-bold leading-tight text-navy">{{ kpi.value }}</div>
              <div class="mt-0.5 text-[10.5px] text-sub">{{ kpi.note }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="mt-4 grid grid-cols-1 gap-4 xl:grid-cols-3">
        <!-- 播放趋势 -->
        <div class="card anim xl:col-span-2">
          <div class="card-h">
            <div>
              <div class="card-t">播放趋势</div>
              <div class="card-s">实线为播放量 · 虚线为按播放量加权的完播率</div>
            </div>
            <span class="pill pill-blue">近 {{ data.days }} 天</span>
          </div>
          <div class="card-b">
            <MerchantChart :option="trendOption" :height="280" />
          </div>
        </div>

        <!-- 地域分布 -->
        <div class="card anim">
          <div class="card-h">
            <div>
              <div class="card-t">地域分布</div>
              <div class="card-s">按授权区域汇总播放量</div>
            </div>
          </div>
          <div class="card-b">
            <MerchantChart :option="regionOption" :height="280" />
          </div>
        </div>
      </div>

      <div class="mt-4 grid grid-cols-1 gap-4 xl:grid-cols-3">
        <!-- 单视频对比 -->
        <div class="card anim xl:col-span-2">
          <div class="card-h">
            <div>
              <div class="card-t">单视频播放对比</div>
              <div class="card-s">近 14 天累计播放 TOP 6</div>
            </div>
          </div>
          <div class="card-b">
            <MerchantChart :option="videoBarOption" :height="240" />
          </div>
        </div>

        <!-- 完播率排行 -->
        <div class="card anim">
          <div class="card-h">
            <div>
              <div class="card-t">完播率明细</div>
              <div class="card-s">按播放量降序 · 平均观看时长来自日统计</div>
            </div>
          </div>
          <div class="card-b space-y-2">
            <div
              v-for="v in data?.videos || []"
              :key="v.videoId"
              class="flex items-center gap-2 rounded-lg border border-line px-3 py-2"
            >
              <i class="fa-solid fa-clapperboard text-[13px] text-brand-500"></i>
              <span class="min-w-0 flex-1 truncate text-[12px] font-medium text-ink" :title="v.name">{{ v.name }}</span>
              <span class="num text-[11.5px] text-sub">{{ wan(v.plays) }} 次</span>
              <span
                class="num rounded px-1.5 py-0.5 text-[11px] font-bold"
                :class="v.finishRate == null ? 'bg-slate-100 text-slate-400' : v.finishRate >= 60 ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'"
              >{{ v.finishRate == null ? '—' : `${v.finishRate}%` }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="grid h-64 place-items-center text-[13px] text-sub">
      <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载播放统计…
    </div>
  </div>
</template>
