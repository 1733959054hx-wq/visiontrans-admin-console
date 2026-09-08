<script setup>
import { computed } from 'vue'

const props = defineProps({
  pct: { type: Number, required: true },
  value: { type: String, required: true },
  label: { type: String, default: '' },
  size: { type: Number, default: 150 },
  sw: { type: Number, default: 14 },
  c1: { type: String, default: '#1E3A8A' },
  c2: { type: String, default: '#0EA5E9' },
  fs: { type: Number, default: 22 },
})

const geometry = computed(() => {
  const r = (props.size - props.sw) / 2
  const c = 2 * Math.PI * r
  const dash = (c * Math.min(props.pct, 100)) / 100
  return { r, c, dash }
})

const gradientId = `donut-${Math.random().toString(36).slice(2, 9)}`
</script>

<template>
  <svg
    :viewBox="`0 0 ${size} ${size}`"
    :width="size"
    :height="size"
    preserveAspectRatio="xMidYMid meet"
    class="mx-auto block"
  >
    <defs>
      <linearGradient :id="gradientId" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" :stop-color="c1" />
        <stop offset="100%" :stop-color="c2" />
      </linearGradient>
    </defs>
    <circle :cx="size / 2" :cy="size / 2" :r="geometry.r" fill="none" stroke="#EEF2F7" :stroke-width="sw" />
    <circle
      :cx="size / 2"
      :cy="size / 2"
      :r="geometry.r"
      fill="none"
      :stroke="`url(#${gradientId})`"
      :stroke-width="sw"
      stroke-linecap="round"
      :stroke-dasharray="`${geometry.dash.toFixed(2)} ${geometry.c.toFixed(2)}`"
      :transform="`rotate(-90 ${size / 2} ${size / 2})`"
    />
    <text
      :x="size / 2"
      :y="size / 2 + 2"
      text-anchor="middle"
      :font-size="fs"
      font-weight="700"
      fill="#0F172A"
      font-family="JetBrains Mono, Consolas, monospace"
    >
      {{ value }}
    </text>
    <text :x="size / 2" :y="size / 2 + 20" text-anchor="middle" font-size="10.5" fill="#64748B">
      {{ label }}
    </text>
  </svg>
</template>
