<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import DonutChart from '@/components/DonutChart.vue'
import EChart from '@/components/EChart.vue'
import KpiCard from '@/components/KpiCard.vue'
import PageHeader from '@/components/PageHeader.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import RankList from '@/components/RankList.vue'
import StatusPill from '@/components/StatusPill.vue'
import { exportCsv, stamp } from '@/utils/csv'
import { useAppStore } from '@/stores/app'
import { useClusterStore } from '@/stores/cluster'
import { useConfirm } from '@/composables/useConfirm'

const app = useAppStore()
const store = useClusterStore()
const { confirmState, askConfirm, resolveConfirm } = useConfirm()

const data = computed(() => store.data)
/** 波形图 / 拓扑视图 切换 */
const viewMode = ref('wave')
/** 延迟分位切换：P50 / P95 / P99（按分位重塑波形形态） */
const percentile = ref('P95')
/** 分位形态参数：level 控制整体水平，k 控制波动压平/放大，spike 为 P99 周期性毛刺 */
const PERCENTILE_SHAPE = {
  P50: { level: 0.62, k: 0.45, spike: 1 },
  P95: { level: 1, k: 1, spike: 1 },
  P99: { level: 1.28, k: 1.7, spike: 1.22 },
}
/** 按分位重塑波形：P50 更低更平、P95 保持原始、P99 抬升且带周期性毛刺 */
const shapeWave = (samples, p) => {
  const cfg = PERCENTILE_SHAPE[p] ?? PERCENTILE_SHAPE.P95
  const mean = samples.reduce((a, b) => a + b, 0) / (samples.length || 1)
  return samples.map((s, i) => {
    let v = (mean + (s - mean) * cfg.k) * cfg.level
    if (cfg.spike > 1 && (i * 7) % 53 === 0) v *= cfg.spike
    return Math.round(v * 10) / 10
  })
}

onMounted(() => {
  if (!data.value) store.load()
})

/* ------------------------------ 图表配置 ------------------------------ */

const waveOption = computed(() => {
  const wave = data.value?.latency?.wave
  if (!wave) return {}
  const layers = wave.layers.map((layer) => ({
    name: layer.name,
    c1: layer.c1,
    c2: layer.c2,
    data: shapeWave(layer.samples, percentile.value),
  }))
  const maxV = Math.max(...layers.flatMap((l) => l.data))
  const xLabels = new Array(wave.points).fill('')
  wave.labels.forEach((label) => {
    xLabels[label.at] = label.t
  })
  return {
    animation: false,
    grid: { left: 56, right: 18, top: 16, bottom: 28 },
    tooltip: { trigger: 'axis', valueFormatter: (v) => `${Math.round(v)} ms`, textStyle: { fontSize: 11 } },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xLabels,
      axisLine: { lineStyle: { color: '#E2E8F0' } },
      axisTick: { show: false },
      axisLabel: { color: '#94A3B8', fontSize: 10.5 },
    },
    yAxis: {
      type: 'value',
      max: Math.ceil((maxV * 1.08) / 10) * 10,
      splitNumber: 4,
      axisLabel: { color: '#94A3B8', fontSize: 10.5, fontFamily: 'JetBrains Mono, monospace' },
      splitLine: { lineStyle: { color: '#EEF2F7' } },
    },
    series: layers.map((layer) => ({
      name: layer.name,
      type: 'line',
      stack: 'latency',
      symbol: 'none',
      data: layer.data,
      lineStyle: { color: layer.c1, width: 1.2, opacity: 0.55 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: layer.c1, opacity: 0.85 },
            { offset: 1, color: layer.c2, opacity: 0.35 },
          ],
        },
      },
    })),
  }
})

const qpsOption = computed(() => {
  const qps = data.value?.qps
  if (!qps) return {}
  return {
    animation: false,
    grid: { left: 56, right: 18, top: 18, bottom: 52 },
    tooltip: { trigger: 'axis', textStyle: { fontSize: 11 } },
    legend: {
      bottom: 4, icon: 'roundRect', itemWidth: 9, itemHeight: 9, itemGap: 18,
      textStyle: { fontSize: 11.5, color: '#64748B' },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: qps.labels,
      axisLine: { lineStyle: { color: '#E2E8F0' } },
      axisTick: { show: false },
      axisLabel: { color: '#94A3B8', fontSize: 10.5 },
    },
    yAxis: {
      type: 'value',
      max: qps.maxAxis,
      splitNumber: 4,
      axisLabel: { color: '#94A3B8', fontSize: 10.5, fontFamily: 'JetBrains Mono, monospace' },
      splitLine: { lineStyle: { color: '#EEF2F7' } },
    },
    series: [
      {
        name: '真实 QPS 吞吐',
        type: 'line',
        smooth: true,
        data: qps.real,
        symbol: 'circle',
        symbolSize: 5,
        itemStyle: { color: '#2563EB', borderColor: '#fff', borderWidth: 1.5 },
        lineStyle: { color: '#2563EB', width: 2.4 },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#2563EB', opacity: 0.28 },
              { offset: 1, color: '#2563EB', opacity: 0 },
            ],
          },
        },
      },
      {
        name: 'LSTM 容量推演',
        type: 'line',
        smooth: true,
        data: qps.forecast,
        symbol: 'circle',
        symbolSize: 5,
        itemStyle: { color: '#0EA5E9', borderColor: '#fff', borderWidth: 1.5 },
        lineStyle: { color: '#0EA5E9', width: 2.4, type: 'dashed' },
      },
      {
        name: `设计容量上限 ${qps.capacity} QPS`,
        type: 'line',
        data: qps.labels.map(() => qps.capacity),
        symbol: 'none',
        itemStyle: { color: '#F59E0B' },
        lineStyle: { color: '#F59E0B', width: 1.6, type: 'dashed' },
      },
    ],
  }
})

/* ------------------------------ 列表处理 ------------------------------ */

const alarmTone = (level) => ({ P1: 'red', P2: 'amber', P3: 'blue' })[level] || 'blue'
const nodeTone = (v, highColor) => (v > 80 ? 'danger' : v > 60 ? 'warn' : highColor)

const matchKeyword = (...values) => {
  const keyword = app.keyword.trim().toLowerCase()
  if (!keyword) return true
  return values.some((v) => String(v ?? '').toLowerCase().includes(keyword))
}

const filteredNodes = computed(() => (data.value?.nodes || []).filter((n) =>
  matchKeyword(n.id, n.zone, n.role, n.status)))
const filteredAlarms = computed(() => (data.value?.alarms || []).filter((a) =>
  matchKeyword(a.time, a.level, a.message, a.result)))

/* ------------------------------ 拓扑视图 ------------------------------ */

const GATEWAY = 'API 网关'

/** 拓扑图：网关 → 可用区 → 容器节点 的横向树。
 *  tree 系列自动布局，节点不重叠、标签不出界；支持拖拽 / 滚轮缩放。 */
const topoOption = computed(() => {
  const list = filteredNodes.value
  if (!list.length) return {}
  const groups = new Map()
  list.forEach((node) => {
    if (!groups.has(node.zone)) groups.set(node.zone, [])
    groups.get(node.zone).push(node)
  })
  const zones = [...groups.keys()]

  const tree = {
    name: GATEWAY,
    symbolSize: 38,
    itemStyle: { color: '#1E3A8A' },
    label: { fontWeight: 600, color: '#1E3A8A' },
    tip: `承载 ${zones.length} 个可用区 · ${list.length} 个服务组`,
    children: zones.map((zone) => {
      const members = groups.get(zone)
      const risky = members.some((n) => n.status !== '健康')
      return {
        name: zone,
        symbolSize: 28,
        itemStyle: { color: risky ? '#F59E0B' : '#2563EB' },
        tip: `${members.length} 个服务组${risky ? ' · 存在高负载节点' : ' · 全部健康'}`,
        children: members.map((node) => ({
          name: node.id,
          symbolSize: 13 + Math.min(Number(node.containers) || 1, 11),
          itemStyle: {
            color: node.status === '健康' ? '#10B981' : '#EF4444',
            borderColor: '#fff',
            borderWidth: 1.5,
          },
          tip: `${node.role}<br/>CPU ${node.cpu}% · GPU ${node.gpu}%<br/>${node.containers} 容器 · ${node.latency}`,
          info: node,
        })),
      }
    }),
  }

  return {
    animation: false,
    tooltip: {
      backgroundColor: 'rgba(255,255,255,.98)',
      borderColor: '#E2E8F0',
      textStyle: { fontSize: 11.5, color: '#334155' },
      formatter: (p) => (p.data?.tip ? `<b>${p.name}</b><br/>${p.data.tip}` : ''),
    },
    series: [
      {
        type: 'tree',
        data: [tree],
        orient: 'LR',
        roam: true,
        expandAndCollapse: false,
        edgeShape: 'curve',
        left: '4%',
        right: '24%',
        top: '5%',
        bottom: '5%',
        symbol: 'circle',
        lineStyle: { color: '#CBD5E1', width: 1.3, curveness: 0.5 },
        label: {
          position: 'left',
          verticalAlign: 'middle',
          align: 'right',
          fontSize: 10.5,
          color: '#64748B',
          distance: 8,
        },
        leaves: {
          label: { position: 'right', verticalAlign: 'middle', align: 'left', fontSize: 10.5, color: '#64748B', distance: 8 },
        },
        emphasis: { focus: 'descendant' },
        initialTreeDepth: -1,
      },
    ],
  }
})

/** 点击拓扑节点：直接打开该节点的编辑弹窗 */
const onTopoClick = (params) => {
  const node = params.data?.info
  if (node) openNodeEdit(node)
}

/* ------------------------------ 容器节点 CRUD ------------------------------ */

const NODE_FIELDS = [
  { key: 'id', label: '节点 ID', type: 'text', required: true, placeholder: 'vt-shanghai-core-03' },
  { key: 'zone', label: '可用区', type: 'text', required: true, placeholder: '华东 1（上海）' },
  { key: 'role', label: '服务角色', type: 'text', required: true, placeholder: '主节点 · GPU 推理' },
  { key: 'containers', label: '容器数', type: 'number', min: 0 },
  { key: 'cpu', label: 'CPU 利用率 (%)', type: 'number', min: 0, max: 100 },
  { key: 'gpu', label: 'GPU / 显存 (%)', type: 'number', min: 0, max: 100 },
  { key: 'latencyMs', label: '平均延迟 (ms)', type: 'number', min: 0 },
  { key: 'status', label: '状态', type: 'select', options: ['健康', '高负载'] },
]

const ALARM_FIELDS = [
  { key: 'time', label: '触发时间', type: 'time', required: true },
  { key: 'level', label: '告警等级', type: 'select', options: ['P1', 'P2', 'P3'] },
  { key: 'message', label: '告警内容', type: 'text', required: true },
  { key: 'result', label: '处置结果', type: 'select', options: ['已自愈', '已回滚', '已处理', '处理中'] },
]

const dialog = reactive({ open: false, title: '', fields: [], record: {}, mode: 'node' })

const openNodeCreate = () => {
  dialog.open = true
  dialog.title = '新增容器节点'
  dialog.fields = NODE_FIELDS.map((f) => ({ ...f, disabled: false }))
  dialog.record = { id: '', zone: '', role: '', containers: 1, cpu: 40, gpu: 30, latencyMs: 180, status: '健康' }
  dialog.mode = 'node'
}

const openNodeEdit = (row) => {
  dialog.open = true
  dialog.title = `编辑容器节点 · ${row.id}`
  // 编辑时节点 ID 作为业务主键不可修改
  dialog.fields = NODE_FIELDS.map((f) => ({ ...f, disabled: f.key === 'id' }))
  dialog.record = { ...row }
  dialog.mode = 'node'
}

const openAlarmCreate = () => {
  dialog.open = true
  dialog.title = '登记告警事件'
  dialog.fields = ALARM_FIELDS
  dialog.record = { time: '', level: 'P3', message: '', result: '处理中' }
  dialog.mode = 'alarm'
}

const submitDialog = async (form) => {
  dialog.open = false
  try {
    if (dialog.mode === 'node') {
      form.id ? await store.updateNode(form) : await store.createNode(form)
    } else {
      await store.createAlarm(form)
    }
  } catch {
    /* 失败提示已在 store 中统一处理 */
  }
}

const removeNode = async (row) => {
  if (await askConfirm(`确定删除节点「${row.id}」？该操作不可撤销。`, '删除节点')) {
    await store.removeNode(row.id).catch(() => {})
  }
}

const removeAlarm = async (row) => {
  if (await askConfirm(`确定删除 ${row.time} 的 ${row.level} 告警？`, '删除告警')) {
    await store.removeAlarm(row.id).catch(() => {})
  }
}

/** 导出容器清单 CSV */
const exportNodes = () => {
  exportCsv(`容器健康度明细-${stamp()}`,
    ['节点 ID', '可用区', '服务角色', '容器数', 'CPU 利用率', 'GPU / 显存', '平均延迟', '状态'],
    filteredNodes.value.map((n) => [n.id, n.zone, n.role, n.containers, `${n.cpu}%`, `${n.gpu}%`, n.latency, n.status]))
}
</script>

<template>
  <div class="p-5">
    <template v-if="data">
      <PageHeader
        title="集群态势感知与推演监控大盘"
        :desc="`全网实时并发、端到端延迟拆解、QPS 吞吐与节点健康度 · 采样周期 ${data.summary.sampling} · ${data.summary.dataDelay}`"
      >
        <template #actions>
          <span
            class="inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1 text-[12px] font-semibold"
            style="background: #ecfdf5; color: #047857"
          >
            <span class="dot-live h-2 w-2 rounded-full bg-emerald-500"></span>{{ data.summary.monitorLabel }}
          </span>
          <button class="btn btn-ghost btn-sm" @click="viewMode = viewMode === 'wave' ? 'topo' : 'wave'">
            <i class="fa-solid" :class="viewMode === 'wave' ? 'fa-diagram-project' : 'fa-chart-line'"></i>
            {{ viewMode === 'wave' ? '拓扑视图' : '波形视图' }}
          </button>
          <button class="btn btn-primary btn-sm" :disabled="store.acting" @click="store.simulate().catch(() => {})">
            <i class="fa-solid fa-flask"></i>容量推演
          </button>
        </template>
      </PageHeader>

      <!-- 4 项核心指标 -->
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <KpiCard v-for="kpi in data.kpis" :key="kpi.label" :kpi="kpi" />
        <div class="card anim p-4">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-[12px] text-sub">{{ data.health.label }}</div>
              <div class="num mt-1.5 text-[28px] font-bold leading-tight text-emerald-600">
                {{ data.health.value }}
              </div>
              <div class="mt-1.5 text-[11px] text-sub">
                在线 {{ data.health.online }} / {{ data.health.total }} 个微服务容器 · 异常
                {{ data.health.abnormal }}
              </div>
            </div>
            <div class="w-[104px]">
              <DonutChart
                :pct="data.health.pct"
                :value="data.health.value"
                :label="data.health.label"
                :size="104"
                :sw="11"
                :fs="17"
                c1="#10B981"
                c2="#0EA5E9"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 延迟拆解 / 拓扑 + 告警流 -->
      <div class="mt-4 grid grid-cols-1 gap-4 xl:grid-cols-3">
        <div class="card anim xl:col-span-2">
          <div class="card-h">
            <div>
              <div class="card-t">
                {{ viewMode === 'wave' ? '端到端延迟拆解波形图' : '集群服务拓扑视图' }}
              </div>
              <div class="card-s">
                {{ viewMode === 'wave'
                  ? '五层管线堆叠耗时 + 端到端总延迟曲线 · 单位 ms'
                  : '按可用区分组的容器拓扑 · 点击节点可直接编辑' }}
              </div>
            </div>
            <div v-if="viewMode === 'wave'" class="flex items-center gap-1">
              <button
                v-for="p in ['P50', 'P95', 'P99']"
                :key="p"
                class="btn btn-sm !px-2.5"
                :class="percentile === p ? 'btn-primary' : 'btn-ghost'"
                @click="percentile = p"
              >
                {{ p }}
              </button>
            </div>
          </div>
          <div class="card-b">
            <EChart v-if="viewMode === 'wave'" :option="waveOption" :height="300" />
            <EChart v-else :option="topoOption" :height="360" @node-click="onTopoClick" />
          </div>
          <div v-if="viewMode === 'topo'" class="card-h flex-wrap !border-b-0 !border-t border-line">
            <span class="inline-flex items-center gap-1.5 text-[11.5px] text-sub">
              <i class="h-2 w-2 rounded-full bg-emerald-500"></i>健康
            </span>
            <span class="inline-flex items-center gap-1.5 text-[11.5px] text-sub">
              <i class="h-2 w-2 rounded-full bg-rose-500"></i>高负载
            </span>
            <span class="inline-flex items-center gap-1.5 text-[11.5px] text-sub">
              <i class="h-2 w-2 rounded-full bg-electric"></i>可用区（含高负载时转橙）
            </span>
            <span class="text-[11.5px] text-sub">节点越大容器越多 · 点击节点可直接编辑</span>
          </div>
          <div v-if="viewMode === 'wave'" class="card-h flex-wrap !border-b-0 !border-t border-line">
            <span class="text-[11.5px] text-sub">{{ data.latency.breakdownText }}</span>
            <span class="text-[11.5px] text-sub">
              合计（{{ percentile }}） <b class="num text-navy">{{ Math.round(data.latency.total * (PERCENTILE_SHAPE[percentile]?.level ?? 1)) }} ms</b> · SLA ≤ {{ data.latency.sla }} ms
            </span>
          </div>
        </div>

        <div class="card anim">
          <div class="card-h">
            <div>
              <div class="card-t">实时告警与自愈事件流</div>
              <div class="card-s">
                自愈率 {{ data.summary.selfHealRate }} · 平均自愈耗时 {{ data.summary.selfHealCost }} · 共
                {{ filteredAlarms.length }} 条
              </div>
            </div>
            <div class="flex items-center gap-1.5">
              <StatusPill text="自动处置" tone="green" />
              <button class="btn btn-ghost btn-sm !px-2 !py-1" title="登记告警" @click="openAlarmCreate">
                <i class="fa-solid fa-plus text-ice"></i>
              </button>
            </div>
          </div>
          <!-- 限高滚动：告警可达数十条，若不封顶会把整行撑高，
               左侧图表卡片因 grid 默认 stretch 而被拉长，图表下方出现大片空白 -->
          <div class="card-b max-h-[344px] space-y-2 overflow-y-auto">
            <div
              v-for="alarm in filteredAlarms"
              :key="alarm.id"
              class="group rounded-lg border border-line p-2.5"
            >
              <div class="flex items-center gap-2">
                <StatusPill :text="alarm.level" :tone="alarmTone(alarm.level)" small />
                <span class="num text-[11px] text-sub">{{ alarm.time }}</span>
                <span class="pill pill-green ml-auto !text-[10px]">
                  <i class="fa-solid fa-circle-check text-[9px]"></i>{{ alarm.result }}
                </span>
                <button
                  class="text-[10px] text-slate-300 opacity-0 transition group-hover:opacity-100 hover:text-rose-500"
                  title="删除告警"
                  @click="removeAlarm(alarm)"
                >
                  <i class="fa-solid fa-trash"></i>
                </button>
              </div>
              <div class="mt-1.5 text-[12px] leading-snug text-ink">{{ alarm.message }}</div>
            </div>
            <div v-if="!filteredAlarms.length" class="py-6 text-center text-[12px] text-sub">暂无匹配的告警</div>
          </div>
        </div>
      </div>

      <!-- 吞吐推演 + 双可用区并发 -->
      <div class="mt-4 grid grid-cols-1 gap-4 xl:grid-cols-3">
        <div class="card anim xl:col-span-2">
          <div class="card-h">
            <div>
              <div class="card-t">核心 API 吞吐量与容量水位推演</div>
              <div class="card-s">
                24 小时真实吞吐 + LSTM 未来 3 小时推演 + 设计容量上限（{{ data.qps.capacity }} QPS）
              </div>
            </div>
          </div>
          <div class="card-b">
            <EChart :option="qpsOption" :height="250" />
          </div>
        </div>
        <div class="card anim">
          <div class="card-h">
            <div>
              <div class="card-t">双可用区并发分布</div>
              <div class="card-s">含端侧离线引擎会话（不计入云端并发）</div>
            </div>
          </div>
          <div class="card-b">
            <RankList :items="data.regions" unit=" 会话" />
          </div>
          <div class="card-h !border-b-0 !border-t border-line">
            <span class="text-[11.5px] text-sub">合计</span>
            <span class="num text-[12.5px] font-semibold text-navy">14,820 会话</span>
          </div>
        </div>
      </div>

      <!-- 容器健康度明细 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">微服务容器健康度明细</div>
            <div class="card-s">{{ data.summary.containerSummary }}</div>
          </div>
          <div class="flex items-center gap-1.5">
            <StatusPill :text="`${filteredNodes.length} / ${data.nodes.length} 条`" tone="green" />
            <button class="btn btn-ghost btn-sm" @click="exportNodes">
              <i class="fa-solid fa-file-export"></i>导出
            </button>
            <button class="btn btn-primary btn-sm" @click="openNodeCreate">
              <i class="fa-solid fa-plus"></i>新增节点
            </button>
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="tb">
            <thead>
              <tr>
                <th>节点 ID</th>
                <th>可用区</th>
                <th>服务角色</th>
                <th>容器数</th>
                <th>CPU 利用率</th>
                <th>GPU / 显存</th>
                <th>平均延迟</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="node in filteredNodes" :key="node.id">
                <td class="num text-[12px] font-medium">{{ node.id }}</td>
                <td class="text-[12px]">{{ node.zone }}</td>
                <td class="text-[12px]">{{ node.role }}</td>
                <td class="num text-[12px]">{{ node.containers }}</td>
                <td><ProgressBar :value="node.cpu" :tone="nodeTone(node.cpu, 'ok')" show-text /></td>
                <td><ProgressBar :value="node.gpu" :tone="nodeTone(node.gpu, '')" show-text /></td>
                <td class="num text-[12px]">{{ node.latency }}</td>
                <td>
                  <StatusPill :text="node.status" :tone="node.status === '健康' ? 'green' : 'amber'" />
                </td>
                <td class="text-right">
                  <button
                    class="btn btn-ghost btn-sm !px-2 !py-1"
                    title="编辑"
                    @click="openNodeEdit(node)"
                  >
                    <i class="fa-solid fa-pen"></i>
                  </button>
                  <button
                    class="btn btn-ghost btn-sm !px-2 !py-1"
                    title="删除"
                    @click="removeNode(node)"
                  >
                    <i class="fa-solid fa-trash text-rose-500"></i>
                  </button>
                </td>
              </tr>
              <tr v-if="!filteredNodes.length">
                <td colspan="9" class="py-8 text-center text-[12px] text-sub">没有匹配的节点</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <div v-else class="grid h-64 place-items-center text-[13px] text-sub">
      <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载集群监控数据…
    </div>

    <CrudDialog
      :open="dialog.open"
      :title="dialog.title"
      :fields="dialog.fields"
      :model-value="dialog.record"
      :loading="store.acting"
      @close="dialog.open = false"
      @submit="submitDialog"
    />
    <ConfirmDialog v-bind="confirmState" @resolve="resolveConfirm" />
  </div>
</template>
