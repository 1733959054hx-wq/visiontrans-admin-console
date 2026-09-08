<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** 0 ~ 100 */
  value: { type: [Number, String], required: true },
  /** ok / warn / danger / 空（默认蓝色渐变） */
  tone: { type: String, default: '' },
  width: { type: String, default: 'w-16' },
  showText: { type: Boolean, default: false },
})

const width = computed(() => {
  const n = parseFloat(props.value)
  return `${Math.max(0, Math.min(100, isNaN(n) ? 0 : n))}%`
})

const color = computed(() => {
  if (props.tone === 'ok') return '#10B981'
  if (props.tone === 'warn') return '#F59E0B'
  if (props.tone === 'danger') return '#EF4444'
  return ''
})
</script>

<template>
  <div class="flex items-center gap-2">
    <div class="bar" :class="width, tone">
      <i :style="color ? `width:${width};background:${color}` : `width:${width}`"></i>
    </div>
    <span v-if="showText" class="num text-[11.5px]">{{ value }}</span>
  </div>
</template>
