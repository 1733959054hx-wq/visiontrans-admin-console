<script setup>
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import * as echarts from 'echarts/core'
import { EffectScatterChart } from 'echarts/charts'
import { GeoComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([EffectScatterChart, GeoComponent, TooltipComponent, CanvasRenderer])

/**
 * 中国地图（异常设备登录热力）：
 * - 省界 GeoJSON 运行时从 DataV 动态加载，不在代码里内置任何地图数据
 * - 点位坐标由后端地理编码服务动态解析
 * - 支持滚轮缩放 / 拖拽平移，异常点位带涟漪高亮
 */
const GEO_JSON_URL = 'https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json'
const TONE = { ok: '#10B981', warn: '#F59E0B', bad: '#EF4444' }

const props = defineProps({
  /** [{ name, lon, lat, sessions, level }] */
  points: { type: Array, default: () => [] },
  height: { type: Number, default: 420 },
})

const host = ref(null)
const chart = shallowRef(null)
const status = ref('loading') // loading | ready | error

const render = () => {
  if (!chart.value || status.value !== 'ready') return
  chart.value.setOption(
    {
      animation: false,
      tooltip: {
        backgroundColor: 'rgba(255,255,255,.98)',
        borderColor: '#E2E8F0',
        textStyle: { fontSize: 11.5, color: '#334155' },
        formatter: (p) => {
          const d = p.data || {}
          const sessions = Number(d.value?.[2] ?? 0).toLocaleString()
          const verdict = d.level === 'bad' ? '异常' : d.level === 'warn' ? '可疑' : '正常'
          return `<b>${d.name ?? p.name}</b><br/>会话 ${sessions} · 判定 ${verdict}`
        },
      },
      geo: {
        map: 'china',
        roam: true,
        zoom: 1.15,
        scaleLimit: { min: 0.8, max: 6 },
        label: { show: false },
        itemStyle: { areaColor: '#EFF6FF', borderColor: '#C9D9F0', borderWidth: 1 },
        emphasis: {
          label: { show: true, fontSize: 10, color: '#334155' },
          itemStyle: { areaColor: '#DBEAFE' },
        },
      },
      series: [
        {
          type: 'effectScatter',
          coordinateSystem: 'geo',
          data: props.points.map((p) => ({
            name: p.name,
            value: [p.lon, p.lat, p.sessions],
            level: p.level,
          })),
          symbolSize: (val) => 9 + Math.min(Math.sqrt(Number(val[2]) || 0) / 3.2, 20),
          rippleEffect: { brushType: 'stroke', scale: 2.4, period: 4 },
          itemStyle: {
            color: (p) => TONE[p.data.level] || TONE.ok,
            borderColor: '#fff',
            borderWidth: 1.5,
            shadowBlur: 8,
            shadowColor: 'rgba(37, 99, 235,.35)',
          },
          label: { show: true, position: 'right', distance: 5, fontSize: 10.5, color: '#334155', formatter: (p) => p.name },
          zlevel: 2,
        },
      ],
    },
    true,
  )
}

const loadGeo = async () => {
  status.value = 'loading'
  try {
    const res = await fetch(GEO_JSON_URL)
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    echarts.registerMap('china', await res.json())
    status.value = 'ready'
    render()
  } catch {
    status.value = 'error'
  }
}

const resize = () => chart.value && chart.value.resize()

onMounted(async () => {
  chart.value = echarts.init(host.value)
  window.addEventListener('resize', resize)
  await loadGeo()
})

watch(() => [props.points, status.value], render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart.value) {
    chart.value.dispose()
    chart.value = null
  }
})
</script>

<template>
  <div class="relative">
    <div ref="host" class="w-full" :style="{ height: `${height}px` }"></div>

    <div
      v-if="status === 'loading'"
      class="absolute inset-0 grid place-items-center bg-white/70 text-[12.5px] text-sub"
    >
      <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>地图数据加载中…
    </div>
    <div v-else-if="status === 'error'" class="absolute inset-0 grid place-items-center bg-white/85">
      <div class="text-center">
        <div class="mb-2 text-[12.5px] text-sub">地图数据加载失败（需访问 geo.datav.aliyun.com）</div>
        <button class="btn btn-ghost btn-sm" @click="loadGeo">
          <i class="fa-solid fa-rotate mr-1"></i>重试
        </button>
      </div>
    </div>

    <div class="pointer-events-none absolute bottom-2 left-3 flex items-center gap-3 text-[11px] text-sub">
      <span class="inline-flex items-center gap-1">
        <i class="h-2 w-2 rounded-full" style="background: #10b981"></i>正常
      </span>
      <span class="inline-flex items-center gap-1">
        <i class="h-2 w-2 rounded-full" style="background: #f59e0b"></i>可疑
      </span>
      <span class="inline-flex items-center gap-1">
        <i class="h-2 w-2 rounded-full" style="background: #ef4444"></i>异常
      </span>
      <span>滚轮缩放 · 拖拽平移</span>
    </div>
  </div>
</template>
