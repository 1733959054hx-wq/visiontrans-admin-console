<script setup>
import { computed } from 'vue'

const props = defineProps({
  text: { type: [String, Number], required: true },
  tone: { type: String, default: 'slate' },
  small: { type: Boolean, default: false },
})

const cls = computed(() => {
  const map = {
    green: 'pill-green',
    blue: 'pill-blue',
    amber: 'pill-amber',
    red: 'pill-red',
    slate: 'pill-slate',
  }
  return map[props.tone] || 'pill-slate'
})
</script>

<template>
  <span class="pill" :class="[cls, small ? '!text-[10px]' : '']">{{ text }}</span>
</template>
