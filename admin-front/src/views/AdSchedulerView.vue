<script setup>
import { computed, onMounted, reactive, watch } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import EChart from '@/components/EChart.vue'
import KpiCard from '@/components/KpiCard.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusPill from '@/components/StatusPill.vue'
import ToggleSwitch from '@/components/ToggleSwitch.vue'
import { exportCsv, stamp } from '@/utils/csv'
import { useAdStore } from '@/stores/ads'
import { useAppStore } from '@/stores/app'
import { useConfirm } from '@/composables/useConfirm'

const app = useAppStore()
const store = useAdStore()
const { confirmState, askConfirm, resolveConfirm } = useConfirm()

const data = computed(() => store.data)

/** 频控滑块本地值（名称 → 数值），change 后提交后端 */
const freqValues = reactive({})
watch(
  () => data.value?.frequency,
  (list) => {
    if (!list) return
    list.forEach((item) => {
      if (!(item.name in freqValues)) freqValues[item.name] = item.value
    })
  },
  { immediate: true },
)

onMounted(() => {
  if (!data.value) store.load()
})

const commitFrequency = (item) => {
  if (freqValues[item.name] !== item.value) {
    store.setFrequency(item.name, freqValues[item.name]).catch(() => {})
  }
}

const SLOT_FIELDS = [
  { key: 'name', label: '广告位名称', type: 'text', required: true, placeholder: '开屏 · 双十一主会场' },
  { key: 'status', label: '库存状态', type: 'select', options: ['已售罄', '部分售出', '预售锁定', '空闲可购'] },
  { key: 'remain', label: '剩余库存', type: 'text', kind: 'percent', suffix: '%', decimals: 1, min: 0, max: 100, placeholder: '100' },
  { key: 'color', label: '状态色', type: 'select', options: ['#EF4444', '#F59E0B', '#14B8A6', '#10B981'] },
  { key: 'ratio', label: '占用比例（-1 表示预售）', type: 'number', min: -1, max: 100 },
]

const dialog = reactive({ open: false, title: '', fields: [], record: {}, mode: 'create' })

const openCreate = () => {
  dialog.open = true
  dialog.mode = 'create'
  dialog.title = '新建广告位排期'
  dialog.fields = SLOT_FIELDS
  dialog.record = { name: '', status: '空闲可购', remain: '0%', color: '#10B981', ratio: 0 }
}

const openEdit = (row) => {
  dialog.open = true
  dialog.mode = 'edit'
  dialog.title = `编辑广告位 · ${row.name}`
  dialog.fields = SLOT_FIELDS
  dialog.record = {
    id: row.id, name: row.name, status: row.status, remain: row.remain, color: row.color,
    ratio: row.remain === '预售' ? -1 : parseFloat(row.remain) || 0,
  }
}

const submitDialog = async (form) => {
  const mode = dialog.mode
  dialog.open = false
  try {
    if (mode === 'create') await store.createSlot(form)
    else await store.updateSlot(form)
  } catch {
    /* 提示已在 store 统一处理 */
  }
}

const removeSlot = async (row) => {
  if (await askConfirm(`确定删除广告位「${row.name}」？`, '删除广告位')) {
    await store.removeSlot(row.id).catch(() => {})
  }
}

/** 上架 / 下架广告位（后端 PATCH /ads/slots/{id}/online） */
const setOnline = (slot, online) => {
  if (!!slot.online !== online) store.setOnline(slot.id, online).catch(() => {})
}

/** eCPM 矩阵单元格配色 */
const cellStyle = (value) => {
  const t = value / (data.value?.matrix?.max || 92)
  const r = Math.round(248 - (248 - 37) * t)
  const g = Math.round(250 - (250 - 99) * t)
  const b = Math.round(252 - (252 - 235) * t)
  return { background: `rgb(${r},${g},${b})`, color: t > 0.55 ? '#ffffff' : '#0F172A' }
}

const remainTone = { '100%': 'text-rose-500', '0%': 'text-emerald-600', 预售: 'text-brand-500' }

/* ---------------------------- 实时数据看板（曝光 / 点击 / 转化） ---------------------------- */

const realtime = computed(() => data.value?.realtime)

/** 指标卡配色（与后端 tone 字段对应）。 */
const metricColor = {
  blue: '#0D9488',
  green: '#10B981',
  indigo: '#6366F1',
  amber: '#F59E0B',
  emerald: '#059669',
  slate: '#64748B',
}

/** 24 小时曝光 / 点击 / 转化趋势：曝光用左轴，点击与转化共用右轴。 */
const realtimeOption = computed(() => {
  const rt = realtime.value
  if (!rt?.hours?.length) return null
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['曝光量', '点击量', '转化量'], bottom: 0, itemWidth: 10, itemHeight: 8, textStyle: { fontSize: 10 } },
    grid: { left: 6, right: 6, top: 18, bottom: 26, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: rt.hours, axisLabel: { fontSize: 10, interval: 2 } },
    yAxis: [
      { type: 'value', name: '曝光', nameTextStyle: { fontSize: 10 }, axisLabel: { fontSize: 10 }, splitLine: { lineStyle: { type: 'dashed' } } },
      { type: 'value', name: '点击/转化', nameTextStyle: { fontSize: 10 }, axisLabel: { fontSize: 10 }, splitLine: { show: false } },
    ],
    series: [
      {
        name: '曝光量', type: 'line', smooth: true, symbol: 'none', data: rt.impressions,
        lineStyle: { width: 2, color: '#0D9488' }, itemStyle: { color: '#0D9488' },
        areaStyle: { color: 'rgba(13,148,136,0.10)' },
      },
      {
        name: '点击量', type: 'line', smooth: true, symbol: 'none', yAxisIndex: 1, data: rt.clicks,
        lineStyle: { width: 2, color: '#10B981' }, itemStyle: { color: '#10B981' },
      },
      {
        name: '转化量', type: 'bar', yAxisIndex: 1, barMaxWidth: 10, data: rt.conversions,
        itemStyle: { color: 'rgba(99,102,241,0.65)', borderRadius: [2, 2, 0, 0] },
      },
    ],
  }
})

const exportRealtime = () => {
  const rt = realtime.value
  if (!rt) return
  exportCsv(`广告实时数据-${stamp()}`,
    ['小时', '曝光量', '点击量', '转化量'],
    rt.hours.map((h, i) => [h, rt.impressions[i] ?? 0, rt.clicks[i] ?? 0, rt.conversions[i] ?? 0]))
}

const exportSlotPerformance = () => {
  exportCsv(`广告位投放表现-${stamp()}`,
    ['广告位', '曝光量', '点击量', '转化量', '点击率(%)', '转化率(%)'],
    (realtime.value?.topSlots || []).map((s) => [s.name, s.impressions, s.clicks, s.conversions,
      s.ctr.toFixed(2), s.cvr.toFixed(2)]))
}

const matchKeyword = (...values) => {
  const keyword = app.keyword.trim().toLowerCase()
  if (!keyword) return true
  return values.some((v) => String(v ?? '').toLowerCase().includes(keyword))
}

const filteredSlots = computed(() => (data.value?.slots || [])
  .filter((s) => matchKeyword(s.name, s.status, s.remain)))

const exportSlots = () => {
  exportCsv(`广告位排期-${stamp()}`,
    ['广告位', '库存状态', '剩余库存'],
    filteredSlots.value.map((s) => [s.name, s.status, s.remain]))
}
</script>

<template>
  <div class="p-5">
    <template v-if="data">
      <PageHeader
        title="全网广告位排期与调度引擎"
        desc="曝光 / 点击 / 转化实时数据看板 · 12 类广告位库存甘特排期 · 场景×语种 eCPM 策略矩阵 · 单用户跨广告位联合频控"
      >
        <template #actions>
          <button class="btn btn-ghost btn-sm" @click="openCreate">
            <i class="fa-solid fa-calendar-plus"></i>新建排期
          </button>
          <button class="btn btn-primary btn-sm" :disabled="store.acting" @click="store.adopt().catch(() => {})">
            <i class="fa-solid fa-wand-magic-sparkles"></i>AI 智能调优
          </button>
        </template>
      </PageHeader>

      <!-- KPI -->
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <KpiCard v-for="kpi in data.kpis" :key="kpi.label" :kpi="kpi" />
      </div>

      <!-- 实时数据看板：曝光 / 点击 / 转化 -->
      <div v-if="realtime" class="card anim mt-4">
        <div class="card-h flex-wrap">
          <div>
            <div class="card-t">广告实时数据看板</div>
            <div class="card-s">
              曝光 / 点击 / 转化全链路指标 · 数据取自 metric_sample 逐小时真实采样
              <span v-if="realtime.updatedAt" class="ml-1">· 更新于 {{ realtime.updatedAt }}</span>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <button class="btn btn-ghost btn-sm" @click="exportRealtime">
              <i class="fa-solid fa-file-export"></i>导出趋势
            </button>
            <button class="btn btn-ghost btn-sm" @click="exportSlotPerformance">
              <i class="fa-solid fa-file-export"></i>导出广告位表现
            </button>
          </div>
        </div>

        <!-- 指标卡 -->
        <div class="card-b grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
          <div
            v-for="metric in realtime.metrics"
            :key="metric.name"
            class="rounded-xl border border-line bg-slate-50/60 px-3 py-2.5"
          >
            <div class="flex items-center gap-1.5 text-[11px] text-sub">
              <i class="fa-solid" :class="metric.icon" :style="`color:${metricColor[metric.tone] || '#64748B'}`"></i>
              {{ metric.name }}
            </div>
            <div class="num mt-1 text-[17px] font-bold leading-none text-navy">
              {{ metric.value }}<span class="ml-0.5 text-[10.5px] font-normal text-sub">{{ metric.unit }}</span>
            </div>
            <div class="mt-1.5 text-[10.5px] leading-snug text-sub">{{ metric.desc }}</div>
          </div>
        </div>

        <!-- 24 小时趋势 -->
        <div class="grid grid-cols-1 gap-4 px-4 pb-4 xl:grid-cols-3">
          <div class="xl:col-span-2">
            <div class="mb-1 text-[12px] font-semibold text-ink">全天投放趋势</div>
            <EChart v-if="realtimeOption" :option="realtimeOption" :height="240" />
          </div>
          <div>
            <div class="mb-1 text-[12px] font-semibold text-ink">广告位表现 Top 6</div>
            <div class="space-y-1.5">
              <div
                v-for="slot in realtime.topSlots"
                :key="slot.name"
                class="rounded-lg border border-line px-2.5 py-2"
              >
                <div class="flex items-center justify-between gap-2">
                  <span class="truncate text-[12px] text-ink">{{ slot.name }}</span>
                  <StatusPill :text="`CTR ${slot.ctr.toFixed(2)}%`" :tone="slot.ctr > 6 ? 'green' : 'blue'" small />
                </div>
                <div class="mt-1 flex items-center gap-3 text-[10.5px] text-sub">
                  <span>曝光 <b class="num text-ink">{{ slot.impressions.toLocaleString() }}</b></span>
                  <span>点击 <b class="num text-ink">{{ slot.clicks.toLocaleString() }}</b></span>
                  <span>转化 <b class="num text-ink">{{ slot.conversions.toLocaleString() }}</b></span>
                  <span class="ml-auto">CVR {{ slot.cvr.toFixed(2) }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 甘特排期 + 频控 -->
      <div class="mt-4 grid grid-cols-1 gap-4 xl:grid-cols-3">
        <div class="card anim xl:col-span-2">
          <div class="card-h flex-wrap">
            <div>
              <div class="card-t">广告位库存甘特图</div>
              <div class="card-s">
                {{ filteredSlots.length }} 广告位 × {{ data.days.length }} 天 · 点击行尾按钮可编辑 / 删除
              </div>
            </div>
            <div class="flex flex-wrap items-center gap-3 text-[11px] text-sub">
              <span class="inline-flex items-center gap-1.5"><i class="h-2.5 w-2.5 rounded-sm" style="background: #ef4444"></i>售罄</span>
              <span class="inline-flex items-center gap-1.5"><i class="h-2.5 w-2.5 rounded-sm" style="background: #f59e0b"></i>部分售出</span>
              <span class="inline-flex items-center gap-1.5"><i class="h-2.5 w-2.5 rounded-sm" style="background: #14B8A6"></i>预售锁定</span>
              <span class="inline-flex items-center gap-1.5"><i class="h-2.5 w-2.5 rounded-sm" style="background: #10b981"></i>空闲可购</span>
            </div>
          </div>
          <div class="card-b overflow-x-auto">
            <table class="w-full text-[11.5px]">
              <thead>
                <tr class="text-sub">
                  <th class="py-1.5 pr-3 text-left font-medium">广告位</th>
                  <th v-for="day in data.days" :key="day" class="px-1 font-medium">{{ day }}</th>
                  <th class="pl-3 font-medium">剩余库存</th>
                  <th class="pl-3 text-center font-medium">上线</th>
                  <th class="pl-3 text-right font-medium">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="slot in filteredSlots" :key="slot.id" class="group">
                  <td class="whitespace-nowrap py-1.5 pr-3 text-left text-ink">{{ slot.name }}</td>
                  <td v-for="(cell, index) in slot.cells" :key="index" class="px-1 py-1">
                    <div
                      class="h-5 rounded"
                      :class="cell ? '' : 'border border-dashed border-line'"
                      :style="cell ? `background:${slot.color}` : ''"
                    ></div>
                  </td>
                  <td class="num pl-3 font-semibold" :class="remainTone[slot.remain] || 'text-amber-600'">
                    {{ slot.remain }}
                  </td>
                  <td class="pl-3 text-center">
                    <ToggleSwitch
                      :model-value="!!slot.online"
                      :disabled="store.acting"
                      @update:model-value="(v) => setOnline(slot, v)"
                    />
                  </td>
                  <td class="pl-3 text-right">
                    <button
                      class="text-[11px] text-slate-300 transition group-hover:opacity-100 hover:text-electric"
                      title="编辑"
                      @click="openEdit(slot)"
                    >
                      <i class="fa-solid fa-pen"></i>
                    </button>
                    <button
                      class="ml-2 text-[11px] text-slate-300 transition hover:text-rose-500"
                      title="删除"
                      @click="removeSlot(slot)"
                    >
                      <i class="fa-solid fa-trash"></i>
                    </button>
                  </td>
                </tr>
                <tr v-if="!filteredSlots.length">
                  <td :colspan="data.days.length + 4" class="py-8 text-center text-[12px] text-sub">
                    没有匹配的广告位
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="card anim">
          <div class="card-h">
            <div>
              <div class="card-t">单用户频次限制</div>
              <div class="card-s">跨广告位联合频控（拖动后自动保存）</div>
            </div>
          </div>
          <div class="card-b space-y-3">
            <div v-for="cap in data.frequency" :key="cap.name">
              <div class="mb-1 flex items-center justify-between text-[12px]">
                <span class="text-sub">{{ cap.name }}</span>
                <span class="num font-semibold text-navy">{{ freqValues[cap.name] }}</span>
              </div>
              <input
                v-model.number="freqValues[cap.name]"
                type="range"
                class="rng"
                :min="0"
                :max="cap.max"
                @change="commitFrequency(cap)"
              />
            </div>
            <div class="space-y-1.5 border-t border-line pt-2">
              <div
                v-for="strategy in data.freqStrategies"
                :key="strategy.name"
                class="flex items-center justify-between text-[12px]"
              >
                <span class="text-sub">{{ strategy.name }}</span>
                <span class="inline-flex items-center gap-1.5" :class="strategy.enabled ? 'text-electric' : 'text-sub'">
                  <i class="fa-solid" :class="strategy.enabled ? 'fa-toggle-on text-electric' : 'fa-toggle-off text-slate-300'"></i>
                  {{ strategy.enabled ? '已开启' : '已关闭' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- eCPM 策略矩阵 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">场景定向 × 语种对 eCPM 策略矩阵</div>
            <div class="card-s">
              {{ data.matrix.scenes.length }} × {{ data.matrix.langs.length }} 格策略单元 · 颜色越深 eCPM 越高（单位：指数）
            </div>
          </div>
          <div class="flex items-center gap-2 text-[11px] text-sub">
            <span>低</span>
            <span class="h-2 w-24 rounded" style="background: linear-gradient(90deg, #f8fafc, #0D9488)"></span>
            <span>高</span>
            <button class="btn btn-ghost btn-sm !px-2 !py-1" title="导出排期" @click="exportSlots">
              <i class="fa-solid fa-file-export"></i>
            </button>
          </div>
        </div>
        <div class="card-b overflow-x-auto">
          <!-- table-fixed + w-full：让热力格子铺满卡片宽度，避免矩阵挤在左侧留大片空白 -->
          <table class="w-full table-fixed text-[12px]">
            <thead>
              <tr class="text-sub">
                <th class="w-[96px] pr-2"></th>
                <th v-for="lang in data.matrix.langs" :key="lang" class="px-1.5 font-medium">{{ lang }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(scene, i) in data.matrix.scenes" :key="scene">
                <td class="whitespace-nowrap py-2.5 pr-2 font-medium text-ink">{{ scene }}</td>
                <td
                  v-for="(lang, j) in data.matrix.langs"
                  :key="lang"
                  class="num px-1.5 py-2.5 text-center font-semibold"
                  :style="cellStyle(data.matrix.values[i][j])"
                >
                  {{ data.matrix.values[i][j] }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="card-h flex-wrap !border-b-0 !border-t border-line">
          <span class="text-[11.5px] text-sub">
            <i class="fa-solid fa-wand-magic-sparkles mr-1 text-ice"></i>AI 建议：{{ data.advice.text }}
            <StatusPill v-if="data.advice.adopted" text="已采纳" tone="green" small />
          </span>
          <button
            class="btn btn-primary btn-sm"
            :disabled="store.acting || data.advice.adopted"
            @click="store.adopt().catch(() => {})"
          >
            <i class="fa-solid fa-check"></i>采纳建议
          </button>
        </div>
      </div>
    </template>

    <div v-else class="grid h-64 place-items-center text-[13px] text-sub">
      <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载广告排期数据…
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
