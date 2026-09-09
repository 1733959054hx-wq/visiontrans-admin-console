<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** [{ name, sub, value, delta }] */
  items: { type: Array, default: () => [] },
  /** 数值后缀，例如 " 会话" */
  unit: { type: String, default: '' },
})

const maxValue = computed(() => Math.max(...props.items.map((item) => item.value), 1))

const fmt = (n) => Number(n || 0).toLocaleString('en-US')
</script>

<template>
  <div>
    <div v-for="(item, index) in items" :key="item.name" class="flex items-center gap-3 py-[7px]">
      <div
        class="num grid h-5 w-5 flex-none place-items-center rounded-md text-[10.5px] font-bold"
        :style="
          index < 3
            ? 'background:linear-gradient(135deg,#115E59,#14B8A6);color:#fff'
            : 'background:#E5EAF0;color:#64748B'
        "
      >
        {{ index + 1 }}
      </div>
      <div class="w-[38%] min-w-0">
        <div class="truncate text-[12.5px] font-medium text-ink">{{ item.name }}</div>
        <div class="truncate text-[10.5px] text-sub">{{ item.sub }}</div>
      </div>
      <div class="bar flex-1">
        <i :style="`width:${((item.value / maxValue) * 100).toFixed(1)}%`"></i>
      </div>
      <div class="num w-[86px] text-right text-[12.5px] font-semibold text-ink">
        {{ fmt(item.value) }}{{ unit }}
      </div>
      <div
        class="w-12 text-right text-[11px] font-semibold"
        :class="item.delta >= 0 ? 'text-emerald-600' : 'text-rose-500'"
      >
        {{ item.delta >= 0 ? '+' : '' }}{{ item.delta }}%
      </div>
    </div>
  </div>
</template>
