<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import KpiCard from '@/components/KpiCard.vue'
import PageHeader from '@/components/PageHeader.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import StatusPill from '@/components/StatusPill.vue'
import ToggleSwitch from '@/components/ToggleSwitch.vue'
import { exportCsv, stamp } from '@/utils/csv'
import { useAppStore } from '@/stores/app'
import { useModelStore } from '@/stores/models'
import { useConfirm } from '@/composables/useConfirm'

const app = useAppStore()
const store = useModelStore()
const { confirmState, askConfirm, resolveConfirm } = useConfirm()

const data = computed(() => store.data)

/** 灰度比例滑块（本地值，点击保存或失焦后提交） */
const grayRatio = ref(35)
watch(
  () => data.value?.grayscale?.ratio,
  (value) => {
    if (typeof value === 'number') grayRatio.value = value
  },
  { immediate: true },
)

onMounted(() => {
  if (!data.value) store.load()
})

const batchTone = { 已通过: 'green', 观察中: 'amber', 未开始: 'blue', 待触发: 'slate' }
const statusTone = { green: 'green', amber: 'amber', blue: 'blue' }

const MODEL_FIELDS = [
  { key: 'name', label: '模型名称（含版本）', type: 'text', required: true, placeholder: 'NMT-Edge-jaXX v1.0.0' },
  { key: 'type', label: '模型类型', type: 'select', options: ['端侧 OCR', '端侧翻译', '云端翻译', '端侧 ASR', '端侧手语', '端侧空间渲染', '端侧漫画 OCR'] },
  { key: 'precision', label: '量化精度', type: 'select', options: ['INT8', 'FP16'] },
  { key: 'size', label: '包体积', type: 'text', kind: 'decimal', suffix: 'MB', decimals: 1, min: 0, placeholder: '3.4' },
  { key: 'coverage', label: '端侧覆盖率', type: 'text', kind: 'percent', suffix: '%', decimals: 1, min: 0, max: 100, placeholder: '92.1' },
  { key: 'grayRatio', label: '灰度比例 (%)', type: 'number', min: 0, max: 100 },
  { key: 'status', label: '发布状态', type: 'select', options: ['全量', '灰度 A 已通过', '灰度 B 观察中', '灰度 C 未开始', '已回滚'] },
]

const dialog = reactive({ open: false, title: '', fields: [], record: {}, mode: 'create' })

const openCreate = () => {
  dialog.open = true
  dialog.mode = 'create'
  dialog.title = '登记新模型版本（训练数据接入）'
  dialog.fields = MODEL_FIELDS.map((f) => ({ ...f, disabled: false }))
  dialog.record = { name: '', type: '端侧 OCR', precision: 'INT8', size: '1.0 MB', coverage: '0%', grayRatio: 0, status: '灰度 C 未开始' }
}

const openEdit = (row) => {
  dialog.open = true
  dialog.mode = 'edit'
  dialog.title = `编辑模型 · ${row.name}`
  // 模型名称作为业务主键，编辑时不允许修改
  dialog.fields = MODEL_FIELDS.map((f) => ({ ...f, disabled: f.key === 'name' }))
  dialog.record = { ...row }
}

const submitDialog = async (form) => {
  const mode = dialog.mode
  dialog.open = false
  try {
    if (mode === 'create') {
      await store.create(form)
    } else {
      await store.update(form)
    }
  } catch {
    /* 提示已在 store 统一处理 */
  }
}

const removeModel = async (row) => {
  if (await askConfirm(`确定从纳管清单中删除模型「${row.name}」？`, '删除模型版本')) {
    await store.remove(row.name).catch(() => {})
  }
}

const toggleStrategy = async (item) => {
  await store.setStrategy(item.name, !item.enabled).catch(() => {})
}

const exportModels = () => {
  exportCsv(`模型清单-${stamp()}`,
    ['模型', '类型', '精度', '包体积', '端侧覆盖率', '灰度比例', '状态'],
    filteredModels.value.map((m) => [m.name, m.type, m.precision, m.size, m.coverage, `${m.grayRatio}%`, m.status]))
}

const matchKeyword = (...values) => {
  const keyword = app.keyword.trim().toLowerCase()
  if (!keyword) return true
  return values.some((v) => String(v ?? '').toLowerCase().includes(keyword))
}

const filteredModels = computed(() => (data.value?.models || [])
  .filter((m) => matchKeyword(m.name, m.type, m.precision, m.status)))
</script>

<template>
  <div class="p-5">
    <template v-if="data">
      <PageHeader
        title="AI 模型生命周期与热更中心"
        desc="端侧量化模型与云端大模型统一纳管 · 秒级热更 · 一键回滚"
      >
        <template #actions>
          <button class="btn btn-ghost btn-sm" @click="exportModels">
            <i class="fa-solid fa-download"></i>导出清单
          </button>
          <button class="btn btn-primary btn-sm" :disabled="store.acting" @click="store.setGrayscale(grayRatio)">
            <i class="fa-solid fa-rotate"></i>版本发布
          </button>
        </template>
      </PageHeader>

      <!-- KPI -->
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <KpiCard v-for="kpi in data.kpis" :key="kpi.label" :kpi="kpi" />
      </div>

      <!-- 模型部署与灰度状态 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">模型部署与灰度状态</div>
            <div class="card-s">支持新增 / 编辑 / 删除模型版本，以及秒级热更与一键回滚</div>
          </div>
          <button class="btn btn-ghost btn-sm" @click="openCreate">
            <i class="fa-solid fa-list-check"></i>训练数据接入
          </button>
        </div>
        <div class="overflow-x-auto">
          <table class="tb">
            <thead>
              <tr>
                <th>模型</th>
                <th>类型</th>
                <th>精度</th>
                <th>包体积</th>
                <th>端侧覆盖率</th>
                <th>灰度进度</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="model in filteredModels" :key="model.name">
                <td class="text-[12.5px] font-medium">
                  <i
                    class="fa-solid mr-1.5"
                    :class="model.icon"
                    :style="{ color: model.iconTone === 'green' ? '#10B981' : '#F59E0B' }"
                  ></i
                  >{{ model.name }}
                </td>
                <td class="text-[12px]">{{ model.type }}</td>
                <td><StatusPill :text="model.precision" tone="blue" /></td>
                <td class="num text-[12px]">{{ model.size }}</td>
                <td class="num text-[12px]">{{ model.coverage }}</td>
                <td>
                  <div class="flex items-center gap-2">
                    <div class="bar w-24"><i :style="`width:${model.grayRatio}%`"></i></div>
                    <span class="num text-[11.5px] text-sub">{{ model.grayRatio }}%</span>
                  </div>
                </td>
                <td>
                  <StatusPill :text="model.status" :tone="statusTone[model.statusTone] || 'slate'" />
                </td>
                <td>
                  <div class="flex items-center justify-end gap-1">
                    <button
                      class="btn btn-ghost btn-sm !px-2 !py-1"
                      title="秒级热更"
                      :disabled="store.acting"
                      @click="store.hotUpdate(model.name).catch(() => {})"
                    >
                      <i class="fa-solid fa-bolt text-ice"></i>
                    </button>
                    <button
                      class="btn btn-ghost btn-sm !px-2 !py-1"
                      title="一键回滚"
                      :disabled="store.acting"
                      @click="store.rollback(model.name).catch(() => {})"
                    >
                      <i class="fa-solid fa-rotate-left text-rose-500"></i>
                    </button>
                    <button class="btn btn-ghost btn-sm !px-2 !py-1" title="编辑" @click="openEdit(model)">
                      <i class="fa-solid fa-pen"></i>
                    </button>
                    <button class="btn btn-ghost btn-sm !px-2 !py-1" title="删除" @click="removeModel(model)">
                      <i class="fa-solid fa-trash text-rose-500"></i>
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="!filteredModels.length">
                <td colspan="8" class="py-8 text-center text-[12px] text-sub">没有匹配的模型</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 灰度策略 + 版本时间线：1:1 布局，时间线是窄列表，占 2/3 会浪费大片右侧留白 -->
      <div class="mt-4 grid grid-cols-1 gap-4 xl:grid-cols-2">
        <div class="card anim">
          <div class="card-h">
            <div>
              <div class="card-t">灰度下发与热更策略</div>
              <div class="card-s">拖动滑块后点击「秒级热更」保存灰度比例</div>
            </div>
          </div>
          <div class="card-b">
            <div class="mb-1 text-[12px] text-sub">灰度比例</div>
            <div class="flex items-center gap-3">
              <input v-model.number="grayRatio" type="range" class="rng" min="0" max="100" />
              <span class="num w-12 text-right text-[15px] font-bold text-navy">{{ grayRatio }}%</span>
            </div>

            <div class="mt-3 grid grid-cols-2 gap-2">
              <div
                v-for="batch in data.grayscale.batches"
                :key="batch.name"
                class="rounded-lg border border-line p-2.5"
              >
                <div class="flex items-center justify-between">
                  <span class="text-[12px] font-medium">{{ batch.name }}</span>
                  <StatusPill :text="batch.status" :tone="batchTone[batch.status] || 'slate'" small />
                </div>
                <div class="num mt-1 text-[11px] text-sub">{{ batch.ratio }}</div>
              </div>
            </div>

            <div class="mt-3 space-y-1.5">
              <div
                v-for="strategy in data.strategies"
                :key="strategy.name"
                class="flex items-center justify-between text-[12px]"
              >
                <span class="text-sub">{{ strategy.name }}</span>
                <ToggleSwitch
                  :model-value="strategy.enabled"
                  :disabled="store.acting"
                  @update:model-value="() => toggleStrategy(strategy)"
                />
              </div>
            </div>

            <div class="mt-3 flex gap-2">
              <button
                class="btn btn-primary btn-sm flex-1"
                :disabled="store.acting"
                @click="store.setGrayscale(grayRatio).catch(() => {})"
              >
                <i class="fa-solid fa-bolt"></i>秒级热更
              </button>
              <button
                class="btn btn-ghost btn-sm flex-1"
                :disabled="store.acting"
                @click="store.rollback(filteredModels[0]?.name || '').catch(() => {})"
              >
                <i class="fa-solid fa-rotate-left"></i>一键回滚
              </button>
            </div>
          </div>
        </div>

        <div class="card anim">
          <div class="card-h">
            <div>
              <div class="card-t">版本迭代与运维时间线</div>
              <div class="card-s">端到端可追溯的模型生命周期（含你刚才的操作）</div>
            </div>
          </div>
          <div class="card-b">
            <div class="relative pl-5">
              <div class="absolute bottom-1 left-[7px] top-1 w-px bg-line"></div>
              <div v-for="event in data.timeline" :key="event.time + event.title" class="relative pb-4 last:pb-0">
                <span
                  class="absolute -left-[18px] top-1 h-3.5 w-3.5 rounded-full border-2 bg-white"
                  :class="
                    event.tone === 'green'
                      ? 'border-emerald-500'
                      : event.tone === 'rose'
                        ? 'border-rose-500'
                        : 'border-amber-500'
                  "
                ></span>
                <div class="flex items-center gap-2">
                  <span class="text-[12.5px] font-semibold text-ink">{{ event.title }}</span>
                  <span class="num text-[11px] text-sub">{{ event.time }}</span>
                </div>
                <div class="mt-0.5 text-[12px] text-sub">{{ event.desc }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="grid h-64 place-items-center text-[13px] text-sub">
      <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载模型中心数据…
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
