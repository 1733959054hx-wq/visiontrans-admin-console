<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import KpiCard from '@/components/KpiCard.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusPill from '@/components/StatusPill.vue'
import { exportCsv, stamp } from '@/utils/csv'
import { useAppStore } from '@/stores/app'
import { useModerationStore } from '@/stores/moderation'
import { useConfirm } from '@/composables/useConfirm'

const app = useAppStore()
const store = useModerationStore()
const { confirmState, askConfirm, resolveConfirm } = useConfirm()

const data = computed(() => store.data)

const columnTone = { 待初审: 'amber', 术语复核: 'blue', 已发布: 'green' }
const priorityTone = { 高: 'red', 中: 'amber', 低: 'slate' }
const COLUMNS = ['待初审', '术语复核', '已发布']

onMounted(() => {
  if (!data.value) store.load()
})

const TASK_FIELDS = [
  { key: 'title', label: '术语包名称', type: 'text', required: true, placeholder: '国际机场安检术语包 v3.2' },
  { key: 'column', label: '所属看板列', type: 'select', options: COLUMNS },
  { key: 'priority', label: '优先级', type: 'select', options: ['高', '中', '低', '—'] },
  { key: 'owner', label: '负责人', type: 'text', placeholder: '李审核' },
  { key: 'due', label: '截止时间', type: 'datetime-local' },
  { key: 'meta', label: '附加信息', type: 'text', placeholder: '领域：交通 · 海关' },
]

const ASSET_FIELDS = [
  { key: 'name', label: '素材名称', type: 'text', required: true, placeholder: 'AR 开屏 · 双十一' },
  { key: 'confidence', label: '机审置信度', type: 'text', kind: 'percent', suffix: '%', decimals: 1, min: 0, max: 100, placeholder: '91.2' },
  { key: 'verdict', label: '判定结果', type: 'select', options: ['通过', '人工复审', '驳回'] },
]

/** 语种包 / 课程知识包表单 */
const PACKAGE_FIELDS = [
  { key: 'type', label: '类型', type: 'select', options: ['语种包', '课程知识包'] },
  { key: 'name', label: '名称', type: 'text', required: true, placeholder: '西班牙语 · 商务合同语种包' },
  { key: 'source', label: '来源', type: 'text', placeholder: '内容合作方 · 上传' },
  { key: 'meta', label: '说明', type: 'text', placeholder: '含 1200 条术语 · 领域：法律' },
]

const dialog = reactive({ open: false, title: '', fields: [], record: {}, mode: 'task' })

const openTaskCreate = (column = '待初审') => {
  dialog.open = true
  dialog.mode = 'task-create'
  dialog.title = `新建术语包审核任务 · ${column}`
  dialog.fields = TASK_FIELDS
  dialog.record = { title: '', column, priority: '中', owner: '', due: '', meta: '' }
}

const openTaskEdit = (card, column) => {
  dialog.open = true
  dialog.mode = 'task-edit'
  dialog.title = '编辑术语包审核任务'
  dialog.fields = TASK_FIELDS
  dialog.record = { ...card, column }
}

const openAssetCreate = () => {
  dialog.open = true
  dialog.mode = 'asset-create'
  dialog.title = '新增 AR 广告素材'
  dialog.fields = ASSET_FIELDS
  dialog.record = { name: '', confidence: '90.0%', verdict: '人工复审' }
}

const openAssetEdit = (row) => {
  dialog.open = true
  dialog.mode = 'asset-edit'
  dialog.title = '编辑 AR 广告素材'
  dialog.fields = ASSET_FIELDS
  dialog.record = { ...row }
}

const openPackageCreate = () => {
  dialog.open = true
  dialog.mode = 'pkg-create'
  dialog.title = '新增语种包 / 课程知识包'
  dialog.fields = PACKAGE_FIELDS
  dialog.record = { type: '语种包', name: '', source: '', meta: '' }
}

const openPackageEdit = (row) => {
  dialog.open = true
  dialog.mode = 'pkg-edit'
  dialog.title = `编辑知识包 · ${row.name}`
  dialog.fields = PACKAGE_FIELDS
  dialog.record = { ...row }
}

const submitDialog = async (form) => {
  const mode = dialog.mode
  dialog.open = false
  try {
    if (mode === 'task-create') await store.createTask(form)
    else if (mode === 'task-edit') await store.updateTask(form)
    else if (mode === 'asset-create') await store.createAsset(form)
    else if (mode === 'asset-edit') await store.updateAsset(form)
    else if (mode === 'pkg-create') await store.createPackage(form)
    else if (mode === 'pkg-edit') await store.updatePackage({ ...dialog.record, ...form })
  } catch {
    /* 提示已在 store 统一处理 */
  }
}

const moveTask = async (card, column, direction) => {
  await store.moveTask(card.id, direction).catch(() => {})
}

const removeTask = async (card) => {
  if (await askConfirm(`确定删除术语包「${card.title}」？`, '删除审核任务')) {
    await store.removeTask(card.id).catch(() => {})
  }
}

const removeAsset = async (row) => {
  if (await askConfirm(`确定删除素材「${row.name}」？`, '删除素材')) {
    await store.removeAsset(row.id).catch(() => {})
  }
}

/** 素材人工复审（仅「人工复审」行可操作） */
const reviewAsset = async (row, decision) => {
  const label = decision === 'pass' ? '通过' : '驳回'
  if (await askConfirm(`确定将素材「${row.name}」人工复审为「${label}」？`, '素材复审')) {
    await store.reviewAsset(row.id, decision).catch(() => {})
  }
}

/* ------------------------------ 知识包审核 ------------------------------ */

const PKG_TYPES = ['全部', '语种包', '课程知识包']
const pkgFilter = ref('全部')

const filteredPackages = computed(() => (data.value?.packages || [])
  .filter((p) => pkgFilter.value === '全部' || p.type === pkgFilter.value)
  .filter((p) => matchKeyword(p.type, p.name, p.source, p.meta, p.status)))

const passPackage = async (row) => {
  if (await askConfirm(`确定通过「${row.name}」的审核并发布？`, '知识包审核通过')) {
    await store.reviewPackage(row.id, 'pass').catch(() => {})
  }
}

const rejectPackage = async (row) => {
  if (await askConfirm(`确定驳回「${row.name}」？驳回后需重新提交审核。`, '知识包审核驳回')) {
    await store.reviewPackage(row.id, 'reject').catch(() => {})
  }
}

const removePackage = async (row) => {
  if (await askConfirm(`确定删除知识包「${row.name}」？`, '删除知识包')) {
    await store.removePackage(row.id).catch(() => {})
  }
}

const confidenceTone = (value) => (value.startsWith('9') ? 'ok' : value.startsWith('8') ? 'warn' : 'danger')
const confidenceColor = (value) =>
  confidenceTone(value) === 'ok' ? '#10B981' : confidenceTone(value) === 'warn' ? '#F59E0B' : '#EF4444'
const confidenceText = (value) =>
  confidenceTone(value) === 'ok' ? 'text-emerald-600' : confidenceTone(value) === 'warn' ? 'text-amber-600' : 'text-rose-500'

const matchKeyword = (...values) => {
  const keyword = app.keyword.trim().toLowerCase()
  if (!keyword) return true
  return values.some((v) => String(v ?? '').toLowerCase().includes(keyword))
}

const filteredKanban = computed(() => (data.value?.kanban || []).map((column) => ({
  ...column,
  cards: column.cards.filter((card) => matchKeyword(card.title, card.owner, card.meta, card.priority)),
})))

const filteredAssets = computed(() => (data.value?.assets || [])
  .filter((a) => matchKeyword(a.name, a.confidence, a.verdict)))

const exportAssets = () => {
  exportCsv(`素材机审清单-${stamp()}`,
    ['素材', '机审置信度', '判定结果'],
    filteredAssets.value.map((a) => [a.name, a.confidence, a.verdict]))
}
</script>

<template>
  <div class="p-5">
    <template v-if="data">
      <PageHeader
        title="语种术语库审核与 UGC 风控中台"
        desc="多语种术语库审核、AR 广告素材机审与 UGC 违规高亮拦截 · 平均处置耗时 82 ms"
      >
        <template #actions>
          <button class="btn btn-ghost btn-sm" @click="openTaskCreate()">
            <i class="fa-solid fa-file-import"></i>术语库导入
          </button>
          <button class="btn btn-primary btn-sm" @click="openTaskCreate()">
            <i class="fa-solid fa-list-check"></i>新建审核流
          </button>
        </template>
      </PageHeader>

      <!-- KPI -->
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <KpiCard v-for="kpi in data.kpis" :key="kpi.label" :kpi="kpi" />
      </div>

      <!-- 审核看板 -->
      <div class="mt-4 grid grid-cols-1 gap-4 xl:grid-cols-3">
        <div v-for="column in filteredKanban" :key="column.title" class="card anim">
          <div class="card-h">
            <div class="card-t">{{ column.title }}</div>
            <div class="flex items-center gap-1.5">
              <StatusPill :text="column.countLabel" :tone="columnTone[column.title] || 'slate'" />
              <button
                class="btn btn-ghost btn-sm !px-2 !py-1"
                title="新增卡片"
                @click="openTaskCreate(column.title)"
              >
                <i class="fa-solid fa-plus text-ice"></i>
              </button>
            </div>
          </div>
          <div class="card-b space-y-2">
            <div
              v-for="card in column.cards"
              :key="card.id"
              class="group rounded-xl border border-line p-3 transition hover:border-brand-200"
            >
              <div class="flex items-center justify-between">
                <span class="truncate text-[12.5px] font-medium text-ink">{{ card.title }}</span>
                <StatusPill :text="card.priority" :tone="priorityTone[card.priority] || 'slate'" small />
              </div>
              <div class="mt-1 text-[11px] text-sub">{{ card.meta }}</div>
              <div class="mt-2 flex items-center justify-between">
                <span class="text-[11px] text-sub"><i class="fa-solid fa-user mr-1"></i>{{ card.owner }}</span>
                <span class="num text-[11px] text-sub">{{ card.due }}</span>
              </div>
              <div class="mt-2 flex items-center justify-end gap-1 border-t border-[#F1F5F9] pt-2">
                <button
                  class="text-[11px] text-slate-400 transition hover:text-electric"
                  title="移到上一列"
                  :disabled="column.title === COLUMNS[0]"
                  @click="moveTask(card, column.title, 'prev')"
                >
                  <i class="fa-solid fa-angle-left"></i>
                </button>
                <button
                  class="text-[11px] text-slate-400 transition hover:text-electric"
                  title="移到下一列"
                  :disabled="column.title === COLUMNS[COLUMNS.length - 1]"
                  @click="moveTask(card, column.title, 'next')"
                >
                  <i class="fa-solid fa-angle-right"></i>
                </button>
                <span class="flex-1"></span>
                <button class="text-[11px] text-slate-400 transition hover:text-electric" title="编辑" @click="openTaskEdit(card, column.title)">
                  <i class="fa-solid fa-pen"></i>
                </button>
                <button class="text-[11px] text-slate-400 transition hover:text-rose-500" title="删除" @click="removeTask(card)">
                  <i class="fa-solid fa-trash"></i>
                </button>
              </div>
            </div>
            <div v-if="!column.cards.length" class="py-6 text-center text-[12px] text-sub">暂无卡片</div>
          </div>
        </div>
      </div>

      <!-- 语种包 / 课程知识包审核 -->
      <div class="card anim mt-4">
        <div class="card-h flex-wrap">
          <div>
            <div class="card-t">语种包 / 课程知识包审核</div>
            <div class="card-s">语种包与课程知识包发布审核 · 通过后全量下发</div>
          </div>
          <div class="flex items-center gap-1">
            <button
              v-for="t in PKG_TYPES"
              :key="t"
              class="btn btn-sm !px-2.5"
              :class="pkgFilter === t ? 'btn-primary' : 'btn-ghost'"
              @click="pkgFilter = t"
            >
              {{ t }}
            </button>
            <button class="btn btn-primary btn-sm ml-2" @click="openPackageCreate">
              <i class="fa-solid fa-plus"></i>新增
            </button>
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="tb">
            <thead>
              <tr>
                <th>类型</th>
                <th>名称</th>
                <th>来源</th>
                <th>说明</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="pkg in filteredPackages" :key="pkg.id">
                <td><StatusPill :text="pkg.type" tone="blue" /></td>
                <td class="text-[12.5px] font-medium">{{ pkg.name }}</td>
                <td class="text-[12px]">{{ pkg.source }}</td>
                <td class="text-[12px] text-sub">{{ pkg.meta }}</td>
                <td><StatusPill :text="pkg.status" :tone="pkg.statusTone || 'slate'" /></td>
                <td class="whitespace-nowrap text-right">
                  <template v-if="pkg.status === '待审核'">
                    <button
                      class="btn btn-ghost btn-sm !px-2 !py-1"
                      title="通过"
                      :disabled="store.acting"
                      @click="passPackage(pkg)"
                    >
                      <i class="fa-solid fa-check text-emerald-600"></i>通过
                    </button>
                    <button
                      class="btn btn-ghost btn-sm !px-2 !py-1"
                      title="驳回"
                      :disabled="store.acting"
                      @click="rejectPackage(pkg)"
                    >
                      <i class="fa-solid fa-ban text-rose-500"></i>驳回
                    </button>
                  </template>
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="编辑" @click="openPackageEdit(pkg)">
                    <i class="fa-solid fa-pen"></i>
                  </button>
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="删除" @click="removePackage(pkg)">
                    <i class="fa-solid fa-trash text-rose-500"></i>
                  </button>
                </td>
              </tr>
              <tr v-if="!filteredPackages.length">
                <td colspan="6" class="py-8 text-center text-[12px] text-sub">没有匹配的知识包</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 素材机审 + UGC 高亮拦截 -->
      <div class="mt-4 grid grid-cols-1 gap-4 xl:grid-cols-2">
        <div class="card anim">
          <div class="card-h">
            <div>
              <div class="card-t">AR 广告素材机审置信度</div>
              <div class="card-s">置信度 ≥ 90% 绿 · 80–90% 琥珀 · 80% 以下驳回</div>
            </div>
            <div class="flex items-center gap-1.5">
              <button class="btn btn-ghost btn-sm !px-2 !py-1" title="导出" @click="exportAssets">
                <i class="fa-solid fa-file-export"></i>
              </button>
              <button class="btn btn-primary btn-sm" @click="openAssetCreate">
                <i class="fa-solid fa-plus"></i>新增素材
              </button>
            </div>
          </div>
          <div class="card-b space-y-2.5">
            <div v-for="asset in filteredAssets" :key="asset.id" class="group flex items-center gap-3">
              <span class="w-40 truncate text-[12px] text-ink">{{ asset.name }}</span>
              <div class="bar flex-1">
                <i :style="`width:${asset.confidence};background:${confidenceColor(asset.confidence)}`"></i>
              </div>
              <span class="num w-12 text-right text-[12px] font-semibold" :class="confidenceText(asset.confidence)">
                {{ asset.confidence }}
              </span>
              <span
                class="pill w-16 justify-center !text-[10px]"
                :class="
                  asset.verdict === '通过'
                    ? 'pill-green'
                    : asset.verdict === '人工复审'
                      ? 'pill-amber'
                      : 'pill-red'
                "
              >
                {{ asset.verdict }}
              </span>
              <!-- 人工复审：仅「人工复审」行可操作 -->
              <template v-if="asset.verdict === '人工复审'">
                <button
                  class="text-[11px] text-slate-300 transition hover:text-emerald-600"
                  title="复审通过"
                  @click="reviewAsset(asset, 'pass')"
                >
                  <i class="fa-solid fa-check"></i>
                </button>
                <button
                  class="text-[11px] text-slate-300 transition hover:text-rose-500"
                  title="复审驳回"
                  @click="reviewAsset(asset, 'reject')"
                >
                  <i class="fa-solid fa-ban"></i>
                </button>
              </template>
              <button class="text-[11px] text-slate-300 transition hover:text-electric" title="编辑" @click="openAssetEdit(asset)">
                <i class="fa-solid fa-pen"></i>
              </button>
              <button class="text-[11px] text-slate-300 transition hover:text-rose-500" title="删除" @click="removeAsset(asset)">
                <i class="fa-solid fa-trash"></i>
              </button>
            </div>
            <div v-if="!filteredAssets.length" class="py-6 text-center text-[12px] text-sub">暂无素材</div>
          </div>
        </div>

        <div class="card anim">
          <div class="card-h flex-wrap">
            <div>
              <div class="card-t">UGC 违规文本高亮拦截 · 用户退款与争议仲裁</div>
              <div class="card-s">实时流处理 · 平均处置耗时 82 ms · 退款工单 SLA 24h</div>
            </div>
            <button
              class="btn btn-ghost btn-sm"
              :disabled="store.acting"
              @click="store.refund().catch(() => {})"
            >
              <i class="fa-solid fa-gavel"></i>按建议退款
            </button>
          </div>
          <div class="card-b">
            <!-- UGC 高亮片段 -->
            <div class="rounded-xl border border-line bg-rose-50/40 p-3">
              <div class="flex items-center justify-between">
                <span class="text-[12px] font-semibold text-ink">{{ data.ugc.title }}</span>
                <span class="pill pill-red !text-[10px]">{{ data.ugc.level }}</span>
              </div>
              <p class="mt-2 text-[12.5px] leading-relaxed">
                <template v-for="(segment, index) in data.ugc.segments" :key="index">
                  <span v-if="segment.tone === 'rose'" class="rounded bg-rose-200 px-1 text-rose-700">{{ segment.text }}</span>
                  <span v-else-if="segment.tone === 'amber'" class="rounded bg-amber-200 px-1 text-amber-800">{{ segment.text }}</span>
                  <span v-else>{{ segment.text }}</span>
                </template>
              </p>
              <div class="mt-2 flex items-center gap-2 text-[11px] text-sub">
                <i class="fa-solid fa-crosshairs text-rose-500"></i>
                命中规则：
                <template v-for="(rule, index) in data.ugc.rules" :key="rule.name">
                  <span v-if="index > 0"> · </span>
                  <span class="num">{{ rule.name }}（{{ rule.score.toFixed(2) }}）</span>
                </template>
              </div>
              <div class="mt-2">
                <StatusPill :text="`当前处置：${data.ugc.verdict}`" :tone="data.ugc.verdict === '待处置' ? 'slate' : 'green'" small />
              </div>
              <div class="mt-3 flex gap-2">
                <button class="btn btn-ghost btn-sm flex-1" :disabled="store.acting" @click="store.decide('review').catch(() => {})">
                  <i class="fa-solid fa-check"></i>复核
                </button>
                <button class="btn btn-ghost btn-sm flex-1" :disabled="store.acting" @click="store.decide('pass').catch(() => {})">
                  <i class="fa-solid fa-circle-check"></i>放行
                </button>
                <button class="btn btn-primary btn-sm flex-1" :disabled="store.acting" @click="store.decide('ban').catch(() => {})">
                  <i class="fa-solid fa-ban"></i>封禁
                </button>
              </div>
            </div>

            <!-- 退款仲裁 -->
            <div class="mt-3 rounded-xl border border-line p-3">
              <div class="flex items-center justify-between">
                <span class="text-[12px] font-semibold text-ink">退款仲裁工单</span>
                <span class="flex items-center gap-1.5">
                  <StatusPill :text="data.refund.sla" tone="amber" small />
                  <StatusPill :text="data.refund.status" :tone="data.refund.status === '待仲裁' ? 'slate' : 'green'" small />
                </span>
              </div>
              <div class="mt-2 grid grid-cols-2 gap-2 text-[12px]">
                <div><span class="text-sub">关联订单</span> <span class="num ml-1 font-semibold text-ink">{{ data.refund.orderNo }}</span></div>
                <div><span class="text-sub">实付金额</span> <span class="num ml-1 font-semibold text-ink">{{ data.refund.paid }}</span></div>
                <div><span class="text-sub">建议退款</span> <span class="num ml-1 font-semibold text-rose-500">{{ data.refund.suggestRefund }}</span></div>
                <div><span class="text-sub">仲裁建议</span> <span class="ml-1 font-semibold text-amber-600">{{ data.refund.advice }}</span></div>
              </div>
              <div class="mt-2 text-[11px] text-sub">{{ data.refund.reasons }}</div>
              <button
                class="btn btn-primary btn-sm mt-3 w-full"
                :disabled="store.acting || data.refund.status !== '待仲裁'"
                @click="store.refund().catch(() => {})"
              >
                <i class="fa-solid fa-gavel"></i>
                {{ data.refund.status === '待仲裁' ? '按建议退款' : `已处理：${data.refund.status}` }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="grid h-64 place-items-center text-[13px] text-sub">
      <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载风控中台数据…
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
