<script setup>
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'

import * as echarts from 'echarts/core'
import { GraphChart, LineChart, TreeChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 按需注册（折线 / 面积图 + 拓扑 / 树图），显著减小打包体积
echarts.use([LineChart, GraphChart, TreeChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const emit = defineEmits(['node-click'])

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
  // 图元点击透传：拓扑图据此打开节点编辑抽屉
  chart.value.on('click', (params) => emit('node-click', params))
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
