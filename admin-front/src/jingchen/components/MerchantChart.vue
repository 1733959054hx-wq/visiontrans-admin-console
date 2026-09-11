<script setup>
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'

import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 模块自有按需注册（折线 / 柱状 / 饼图），与主工程 EChart.vue 的图形集不同
echarts.use([LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: Number, default: 260 },
})

const host = ref(null)
const chart = shallowRef(null)

const render = () => {
  if (chart.value) {
    chart.value.setOption(props.option, true)
  }
}

const resize = () => chart.value && chart.value.resize()

onMounted(() => {
  chart.value = echarts.init(host.value)
  render()
  window.addEventListener('resize', resize)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart.value && chart.value.dispose()
  chart.value = null
})
</script>

<template>
  <div ref="host" class="w-full" :style="{ height: `${height}px` }"></div>
</template>
