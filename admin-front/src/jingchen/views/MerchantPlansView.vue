<script setup>
import { computed, onMounted, reactive } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusPill from '@/components/StatusPill.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useMerchantStore } from '@/jingchen/stores/merchant'

/**
 * 商户投放计划管理（jingchen 模块业务页）。
 *
 * 能力：计划列表 / 新建 / 编辑 / 暂停恢复 / 删除；
 * 数据走 /api/merchant/plans（@RequireRole(MERCHANT)，管理员令牌不可达）。
 */
const store = useMerchantStore()
const { confirmState, askConfirm, resolveConfirm } = useConfirm()

const plans = computed(() => store.plans || [])

/* ---- 汇总指标 ---- */
const summary = computed(() => {
  const list = plans.value
  const yuan = (n) => (n || 0).toLocaleString('zh-CN')
  return [
    { label: '投放计划', value: String(list.length), unit: '个', icon: 'fa-bullhorn', c1: '#1E3A8A', c2: '#2563EB' },
    { label: '日预算合计', value: `¥ ${yuan(list.reduce((s, p) => s + (p.budget || 0), 0))}`, unit: '', icon: 'fa-coins', c1: '#2563EB', c2: '#0EA5E9' },
    { label: '今日已消耗', value: `¥ ${yuan(list.reduce((s, p) => s + (p.used || 0), 0))}`, unit: '', icon: 'fa-sack-dollar', c1: '#0EA5E9', c2: '#22D3EE' },
    { label: '预算预警', value: String(list.filter((p) => p.status === '预算预警' && !p.paused).length), unit: '个', icon: 'fa-triangle-exclamation', c1: '#F59E0B', c2: '#FB923C' },
  ]
})

const fmtMoney = (n) => `¥ ${(n || 0).toLocaleString('zh-CN')}`
const fmtCtr = (ctr) => (ctr == null ? '—' : `${ctr}%`)
const planTone = (p) => {
  if (p.paused) return { text: '已暂停', tone: 'slate' }
  return (
    { 投放中: { text: '投放中', tone: 'green' }, 预算预警: { text: '预算预警', tone: 'amber' }, 待审核: { text: '待审核', tone: 'blue' } }[
      p.status
    ] || { text: p.status, tone: 'slate' }
  )
}
const barColor = (pct) => (pct > 95 ? '#EF4444' : pct > 80 ? '#F59E0B' : '#2563EB')
const planPct = (p) => (p.budget > 0 ? Math.min(100, Math.round(((p.used || 0) / p.budget) * 100)) : 0)

/* ---- 新增 / 编辑（复用通用 CrudDialog，字段按后端契约配置） ---- */
const PLAN_FIELDS = [
  { key: 'name', label: '计划名称', type: 'text', required: true, placeholder: '如：东京机场口岸 AR 实景导览' },
  { key: 'adForm', label: '广告形式', type: 'select', options: ['AR 街景锁定', 'Banner 信息流', '开屏广告'] },
  { key: 'scene', label: '定向场景', type: 'text', placeholder: '如：机场口岸 · 中→日' },
  { key: 'budget', label: '日预算(元)', type: 'number', min: 0, required: true },
  { key: 'owner', label: '负责人', type: 'text', placeholder: '选填' },
  { key: 'ctr', label: '点击率 CTR', type: 'text', kind: 'decimal', suffix: '%', decimals: 2, min: 0, max: 100, placeholder: '6.82，选填' },
]

const dialog = reactive({ open: false, title: '', record: {}, mode: 'create' })

const openCreate = () => {
  dialog.open = true
  dialog.mode = 'create'
  dialog.title = '新建投放计划'
  dialog.record = { name: '', adForm: 'Banner 信息流', scene: '', budget: 100000, owner: '', ctr: '' }
}

const openEdit = (row) => {
  dialog.open = true
  dialog.mode = 'edit'
  dialog.title = `编辑计划 · ${row.name}`
  dialog.record = { ...row }
}

/** 弹窗提交 → 数值字段归一化为后端期望的 JSON 类型 */
const submitDialog = async (form) => {
  const mode = dialog.mode
  dialog.open = false
  const ctrRaw = String(form.ctr ?? '').replace(/[^\d.-]/g, '')
  const payload = {
    name: form.name,
    adForm: form.adForm,
    scene: form.scene || '',
    budget: Number(form.budget) || 0,
    owner: form.owner || null,
    ctr: ctrRaw === '' ? null : Number(ctrRaw),
  }
  try {
    if (mode === 'create') await store.createPlan(payload)
    else await store.updatePlan(dialog.record.id, payload)
  } catch {
    /* 提示已在 store 统一处理 */
  }
}

const togglePause = async (row) => {
  try {
    if (row.paused) await store.resumePlan(row.id)
    else await store.pausePlan(row.id)
  } catch {
    /* 提示已在 store 统一处理 */
  }
}

const removePlan = async (row) => {
  if (await askConfirm(`确定删除投放计划「${row.name}」？删除后不可恢复。`, '删除投放计划')) {
    await store.deletePlan(row.id).catch(() => {})
  }
}

onMounted(() => {
  if (!store.plans) store.loadPlans()
})
</script>

<template>
  <div class="p-5">
    <PageHeader title="投放计划管理" desc="广告投放计划的新增 / 编辑 / 暂停恢复与删除 · 数据落 ad_plan 表">
      <template #actions>
        <button class="btn btn-primary btn-sm" :disabled="store.acting" @click="openCreate">
          <i class="fa-solid fa-plus"></i>新建投放计划
        </button>
      </template>
    </PageHeader>

    <!-- 汇总指标 -->
    <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
      <div v-for="kpi in summary" :key="kpi.label" class="card anim relative overflow-hidden p-4">
        <div
          class="absolute -right-8 -top-8 h-32 w-32 rounded-full"
          :style="`background:radial-gradient(circle,rgba(${parseInt(kpi.c1.slice(1, 3), 16)},${parseInt(kpi.c1.slice(3, 5), 16)},${parseInt(kpi.c1.slice(5, 7), 16)},0.16),transparent 70%)`"
        ></div>
        <div class="flex items-start justify-between gap-2">
          <div class="min-w-0">
            <div class="text-[12px] text-sub">{{ kpi.label }}</div>
            <div class="num mt-1.5 text-[24px] font-bold leading-tight text-navy">
              {{ kpi.value }}<span v-if="kpi.unit" class="text-[14px] font-normal text-sub">{{ kpi.unit }}</span>
            </div>
          </div>
          <div
            class="grid h-10 w-10 flex-none place-items-center rounded-xl text-[15px] text-white"
            :style="`background:linear-gradient(135deg,${kpi.c1},${kpi.c2})`"
          >
            <i class="fa-solid" :class="kpi.icon"></i>
          </div>
        </div>
      </div>
    </div>

    <!-- 计划列表 -->
    <div class="card anim mt-4">
      <div class="card-h">
        <div>
          <div class="card-t">正在投放的项目列表</div>
          <div class="card-s">共 {{ plans.length }} 个计划 · 支持暂停 / 恢复与删除</div>
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="tb">
          <thead>
            <tr>
              <th>计划名称 / 编号</th>
              <th>广告形式</th>
              <th>定向场景</th>
              <th class="text-right">日预算(元)</th>
              <th class="text-right">已消耗(元)</th>
              <th>消耗进度</th>
              <th class="text-right">CTR</th>
              <th>状态</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="store.plansLoading">
              <td colspan="9" class="py-8 text-center text-[12px] text-sub">
                <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>正在加载投放计划…
              </td>
            </tr>
            <tr v-for="plan in plans" v-else :key="plan.id">
              <td>
                <div class="text-[12.5px] font-medium text-ink">{{ plan.name }}</div>
                <div class="num text-[10.5px] text-sub">{{ plan.planNo }}<template v-if="plan.owner"> · {{ plan.owner }}</template></div>
              </td>
              <td><StatusPill :text="plan.adForm || '—'" tone="blue" small /></td>
              <td class="text-[12px] text-sub">{{ plan.scene || '—' }}</td>
              <td class="num text-[12px]">{{ fmtMoney(plan.budget) }}</td>
              <td class="num text-[12px]">{{ fmtMoney(plan.used) }}</td>
              <td>
                <div class="flex items-center gap-2">
                  <div class="bar w-24"><i :style="`width:${planPct(plan)}%;background:${barColor(planPct(plan))}`"></i></div>
                  <span class="num text-[11.5px] text-sub">{{ planPct(plan) }}%</span>
                </div>
              </td>
              <td class="num text-[12px] text-right">{{ fmtCtr(plan.ctr) }}</td>
              <td><StatusPill :text="planTone(plan).text" :tone="planTone(plan).tone" /></td>
              <td>
                <div class="flex items-center justify-end gap-1">
                  <button
                    class="btn btn-ghost btn-sm !px-2 !py-1"
                    :title="plan.paused ? '恢复投放' : '暂停投放'"
                    :disabled="store.acting"
                    @click="togglePause(plan)"
                  >
                    <i class="fa-solid" :class="plan.paused ? 'fa-play text-emerald-500' : 'fa-pause text-amber-500'"></i>
                  </button>
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="编辑" @click="openEdit(plan)">
                    <i class="fa-solid fa-pen"></i>
                  </button>
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="删除" @click="removePlan(plan)">
                    <i class="fa-solid fa-trash text-rose-500"></i>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!store.plansLoading && !plans.length">
              <td colspan="9" class="py-8 text-center text-[12px] text-sub">还没有投放计划，点击右上「新建投放计划」创建一条</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <CrudDialog
      :open="dialog.open"
      :title="dialog.title"
      :fields="PLAN_FIELDS"
      :model-value="dialog.record"
      :loading="store.acting"
      @close="dialog.open = false"
      @submit="submitDialog"
    />
    <ConfirmDialog v-bind="confirmState" @resolve="resolveConfirm" />
  </div>
</template>
