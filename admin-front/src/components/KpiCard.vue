<script setup>
import { computed } from 'vue'

import SparkLine from './SparkLine.vue'

const props = defineProps({
  /** 对应后端 KpiMetric 视图模型 */
  kpi: { type: Object, required: true },
})

/** hex → rgba（用于卡片右上角光晕） */
const glow = computed(() => {
  const hex = props.kpi.c2 || '#2563EB'
  if (hex[0] !== '#') return hex
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `radial-gradient(circle,rgba(${r},${g},${b},0.16),transparent 70%)`
})

const toneClass = (tone) =>
  ({
    green: 'pill-green',
    blue: 'pill-blue',
    amber: 'pill-amber',
    red: 'pill-red',
    slate: 'pill-slate',
  })[tone] || 'pill-green'
</script>

<template>
  <div class="card anim relative overflow-hidden p-4">
    <div class="absolute -right-8 -top-8 h-32 w-32 rounded-full" :style="`background:${glow}`"></div>
    <div class="flex items-start justify-between gap-2">
      <div class="min-w-0">
        <div class="text-[12px] text-sub">{{ kpi.label }}</div>
        <div class="num mt-1.5 text-[26px] font-bold leading-tight text-navy">
          {{ kpi.value }}<span v-if="kpi.unit" class="text-[15px] font-normal text-sub">{{ kpi.unit }}</span>
        </div>
      </div>
      <div
        class="grid h-10 w-10 flex-none place-items-center rounded-xl text-[15px] text-white"
        :style="`background:linear-gradient(135deg,${kpi.c1},${kpi.c2});box-shadow:0 8px 18px -10px ${kpi.c2}`"
      >
        <i class="fa-solid" :class="kpi.icon"></i>
      </div>
    </div>
    <div class="mt-2 flex items-center gap-2">
      <span v-if="kpi.delta" class="pill" :class="toneClass(kpi.tone)">
        <i class="fa-solid text-[9px]" :class="kpi.up === false ? 'fa-arrow-down' : 'fa-arrow-up'"></i
        >{{ kpi.delta }}
      </span>
      <span class="text-[11px] text-sub">{{ kpi.note }}</span>
    </div>
    <div class="mt-2.5">
      <SparkLine :data="kpi.spark" :color="kpi.c2 || '#2563EB'" :height="34" />
    </div>
  </div>
</template>
