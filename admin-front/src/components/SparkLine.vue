<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
  color: { type: String, default: '#2563EB' },
  height: { type: Number, default: 34 },
})

const path = computed(() => {
  const data = props.data || []
  if (data.length < 2) return { line: '', area: '' }
  const w = 100
  const h = props.height
  const max = Math.max(...data)
  const min = Math.min(...data)
  const range = max - min || 1
  const x = (i) => 2 + (i * (w - 4)) / (data.length - 1)
  const y = (v) => h - 4 - ((v - min) / range) * (h - 9)
  const line = data.map((v, i) => `${i ? 'L' : 'M'}${x(i).toFixed(1)} ${y(v).toFixed(1)}`).join(' ')
  const area = `${line} L ${x(data.length - 1).toFixed(1)} ${h} L 2 ${h} Z`
  return { line, area, w, h }
})

const gradientId = `spark-${Math.random().toString(36).slice(2, 9)}`
</script>

<template>
  <svg
    :viewBox="`0 0 ${path.w || 100} ${height}`"
    width="100%"
    :height="height"
    preserveAspectRatio="none"
  >
    <defs>
      <linearGradient :id="gradientId" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" :stop-color="color" stop-opacity="0.42" />
        <stop offset="100%" :stop-color="color" stop-opacity="0" />
      </linearGradient>
    </defs>
    <path v-if="path.area" :d="path.area" :fill="`url(#${gradientId})`" />
    <path
      v-if="path.line"
      :d="path.line"
      fill="none"
      :stroke="color"
      stroke-width="1.8"
      stroke-linecap="round"
    />
  </svg>
</template>
