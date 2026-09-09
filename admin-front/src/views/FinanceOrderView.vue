<script setup>
import { computed, onMounted, ref } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import KpiCard from '@/components/KpiCard.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusPill from '@/components/StatusPill.vue'
import { exportCsv, stamp } from '@/utils/csv'
import { useAppStore } from '@/stores/app'
import { useFinanceStore } from '@/stores/finance'
import { useConfirm } from '@/composables/useConfirm'

const app = useAppStore()
const store = useFinanceStore()
const { confirmState, askConfirm, resolveConfirm } = useConfirm()

const data = computed(() => store.data)

onMounted(() => {
  if (!data.value) store.load()
})

/* ------------------------------ 订单筛选 ------------------------------ */

const ORDER_TYPES = ['全部', '会员', '课程', '数字内容']
const ORDER_STATUSES = ['全部', '待支付', '已支付', '支付异常', '退款中', '已退款', '已关闭']
/** 终态订单：不再提供处置操作 */
const FINAL_STATES = ['已退款', '已关闭']

const typeFilter = ref('全部')
const statusFilter = ref('全部')

const matchKeyword = (...values) => {
  const keyword = app.keyword.trim().toLowerCase()
  if (!keyword) return true
  return values.some((v) => String(v ?? '').toLowerCase().includes(keyword))
}

const filteredOrders = computed(() => (data.value?.orders || [])
  .filter((o) => typeFilter.value === '全部' || o.type === typeFilter.value)
  .filter((o) => statusFilter.value === '全部' || o.status === statusFilter.value)
  .filter((o) => matchKeyword(o.orderNo, o.customer, o.type, o.status, o.note)))

const canMarkPaid = (order) => ['待支付', '支付异常'].includes(order.status)
const canClose = (order) => !FINAL_STATES.includes(order.status)

const exportOrders = () => {
  exportCsv(`财务订单-${stamp()}`,
    ['订单号', '用户', '类型', '金额', '状态', '下单时间', '备注'],
    filteredOrders.value.map((o) => [o.orderNo, o.customer, o.type, o.amount, o.status, o.created, o.note]))
}

/* ------------------------------ 订单处置 ------------------------------ */

const markPaid = async (order) => {
  if (await askConfirm(`确定将订单「${order.orderNo}」标记为已支付？`, '标记已支付')) {
    await store.handle(order.id, 'markPaid').catch(() => {})
  }
}

const refundOrder = async (order) => {
  if (await askConfirm(`确定对订单「${order.orderNo}」执行退款？`, '订单退款')) {
    await store.handle(order.id, 'refund').catch(() => {})
  }
}

const closeOrder = async (order) => {
  if (await askConfirm(`确定关闭订单「${order.orderNo}」？关闭后不可恢复。`, '关闭订单')) {
    await store.handle(order.id, 'close').catch(() => {})
  }
}

/* ------------------------------ 商户结算 ------------------------------ */

const reconcile = async (row) => {
  if (await askConfirm(`确定完成商户「${row.merchant}」${row.period} 的对账？`, '商户对账')) {
    await store.reconcile(row.id).catch(() => {})
  }
}

const settle = async (row) => {
  if (await askConfirm(`确定向商户「${row.merchant}」结算 ${row.amount}？`, '商户结算')) {
    await store.settle(row.id).catch(() => {})
  }
}

/* ------------------------------ 发票审核 ------------------------------ */

const approveInvoice = async (row) => {
  if (await askConfirm(`确定通过发票申请「${row.applyNo}」并安排开票？`, '发票审核通过')) {
    await store.review(row.id, true).catch(() => {})
  }
}

const rejectInvoice = async (row) => {
  if (await askConfirm(`确定驳回发票申请「${row.applyNo}」？`, '发票审核驳回')) {
    await store.review(row.id, false).catch(() => {})
  }
}

/* ------------------------------ 商户入驻审核 ------------------------------ */

const filteredOnboardings = computed(() => (data.value?.onboardings || [])
  .filter((m) => matchKeyword(m.applyNo, m.merchantName, m.licenseNo, m.contact, m.status)))

const approveOnboarding = async (row) => {
  if (await askConfirm(`确定通过商户「${row.merchantName}」的入驻申请？`, '入驻审核通过')) {
    await store.reviewOnboarding(row.id, true).catch(() => {})
  }
}

const rejectOnboarding = async (row) => {
  if (await askConfirm(`确定驳回商户「${row.merchantName}」的入驻申请？`, '入驻审核驳回')) {
    await store.reviewOnboarding(row.id, false).catch(() => {})
  }
}

const signContract = async (row) => {
  if (await askConfirm(`确定标记商户「${row.merchantName}」的平台合作协议已签署？`, '合同签署')) {
    await store.signOnboarding(row.id).catch(() => {})
  }
}

const exportOnboardings = () => {
  exportCsv(`商户入驻-${stamp()}`,
    ['申请号', '商户名称', '统一社会信用代码', '联系人', '联系电话', '资质文件', '合同状态', '审核状态', '提交时间', '审核人', '审核意见'],
    filteredOnboardings.value.map((m) => [m.applyNo, m.merchantName, m.licenseNo, m.contact, m.phone,
      m.qualification, m.contractStatus, m.status, m.submitted, m.reviewer, m.remark]))
}
</script>

<template>
  <div class="p-5">
    <template v-if="data">
      <PageHeader
        title="财务订单与商户结算中心"
        desc="C 端订单支付与退款处置 · 商户入驻资质与合同审核 · 按账期对账分账 · B 端发票申请人工复核"
      >
        <template #actions>
          <button class="btn btn-ghost btn-sm" @click="exportOnboardings">
            <i class="fa-solid fa-file-export"></i>导出入驻记录
          </button>
          <button class="btn btn-primary btn-sm" @click="exportOrders">
            <i class="fa-solid fa-file-export"></i>导出订单 CSV
          </button>
        </template>
      </PageHeader>

      <!-- KPI -->
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <KpiCard v-for="kpi in data.kpis" :key="kpi.label" :kpi="kpi" />
      </div>

      <!-- 订单管理 -->
      <div class="card anim mt-4">
        <div class="card-h flex-wrap">
          <div>
            <div class="card-t">订单管理</div>
            <div class="card-s">支持标记已付 / 退款 / 关闭处置 · 状态与金额与上方 KPI 实时联动</div>
          </div>
          <div class="flex flex-wrap items-center gap-1">
            <button
              v-for="t in ORDER_TYPES"
              :key="t"
              class="btn btn-sm !px-2.5"
              :class="typeFilter === t ? 'btn-primary' : 'btn-ghost'"
              @click="typeFilter = t"
            >
              {{ t }}
            </button>
            <span class="mx-1.5 h-4 w-px bg-slate-200"></span>
            <button
              v-for="s in ORDER_STATUSES"
              :key="s"
              class="btn btn-sm !px-2.5"
              :class="statusFilter === s ? 'btn-primary' : 'btn-ghost'"
              @click="statusFilter = s"
            >
              {{ s }}
            </button>
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="tb">
            <thead>
              <tr>
                <th>订单号</th>
                <th>用户</th>
                <th>类型</th>
                <th>金额</th>
                <th>状态</th>
                <th>下单时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in filteredOrders" :key="order.id">
                <td class="num text-[12px] font-medium">{{ order.orderNo }}</td>
                <td class="text-[12px]">{{ order.customer }}</td>
                <td><StatusPill :text="order.type" tone="blue" /></td>
                <td class="num text-[12.5px] font-semibold text-navy">{{ order.amount }}</td>
                <td><StatusPill :text="order.status" :tone="order.statusTone || 'slate'" /></td>
                <td class="num text-[11.5px] text-sub">{{ order.created }}</td>
                <td class="whitespace-nowrap text-right">
                  <template v-if="canMarkPaid(order)">
                    <button
                      class="btn btn-ghost btn-sm !px-2 !py-1"
                      :disabled="store.acting"
                      @click="markPaid(order)"
                    >
                      <i class="fa-solid fa-circle-check text-emerald-600"></i>标记已付
                    </button>
                    <button
                      class="btn btn-ghost btn-sm !px-2 !py-1"
                      :disabled="store.acting"
                      @click="refundOrder(order)"
                    >
                      <i class="fa-solid fa-rotate-left text-amber-500"></i>退款
                    </button>
                  </template>
                  <button
                    v-if="canClose(order)"
                    class="btn btn-ghost btn-sm !px-2 !py-1"
                    :disabled="store.acting"
                    @click="closeOrder(order)"
                  >
                    <i class="fa-solid fa-ban text-rose-500"></i>关闭
                  </button>
                </td>
              </tr>
              <tr v-if="!filteredOrders.length">
                <td colspan="7" class="py-8 text-center text-[12px] text-sub">没有匹配的订单</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 商户入驻审核 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">商户入驻审核</div>
            <div class="card-s">资质文件与平台合作协议复核 · 合同未签署的申请不允许通过</div>
          </div>
          <StatusPill :text="`${filteredOnboardings.length} 条申请`" tone="blue" />
        </div>
        <div class="overflow-x-auto">
          <table class="tb">
            <thead>
              <tr>
                <th>申请号</th>
                <th>商户名称</th>
                <th>统一社会信用代码</th>
                <th>联系人</th>
                <th>资质文件</th>
                <th>合同状态</th>
                <th>审核状态</th>
                <th>提交时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in filteredOnboardings" :key="row.id">
                <td class="num text-[12px] font-medium">{{ row.applyNo }}</td>
                <td class="text-[12.5px]">
                  {{ row.merchantName }}
                  <div class="text-[11px] text-sub">{{ row.phone }}</div>
                </td>
                <td class="num text-[11.5px] text-sub">{{ row.licenseNo }}</td>
                <td class="text-[12px]">{{ row.contact }}</td>
                <td class="text-[11.5px] text-sub">{{ row.qualification }}</td>
                <td><StatusPill :text="row.contractStatus" :tone="row.contractTone || 'slate'" /></td>
                <td>
                  <StatusPill :text="row.status" :tone="row.statusTone || 'slate'" />
                  <div v-if="row.reviewer !== '—'" class="mt-1 text-[10.5px] text-sub">
                    {{ row.reviewer }}：{{ row.remark }}
                  </div>
                </td>
                <td class="num text-[11.5px] text-sub">{{ row.submitted }}</td>
                <td class="whitespace-nowrap text-right">
                  <template v-if="row.status === '待审核'">
                    <button
                      v-if="row.contractStatus === '待签署'"
                      class="btn btn-ghost btn-sm !px-2 !py-1"
                      :disabled="store.acting"
                      @click="signContract(row)"
                    >
                      <i class="fa-solid fa-file-signature"></i>标记签署
                    </button>
                    <button
                      class="btn btn-ghost btn-sm !px-2 !py-1"
                      :disabled="store.acting"
                      @click="approveOnboarding(row)"
                    >
                      <i class="fa-solid fa-check text-emerald-600"></i>通过
                    </button>
                    <button
                      class="btn btn-ghost btn-sm !px-2 !py-1"
                      :disabled="store.acting"
                      @click="rejectOnboarding(row)"
                    >
                      <i class="fa-solid fa-ban text-rose-500"></i>驳回
                    </button>
                  </template>
                  <span v-else class="text-[11px] text-slate-300">已处理</span>
                </td>
              </tr>
              <tr v-if="!filteredOnboardings.length">
                <td colspan="9" class="py-8 text-center text-[12px] text-sub">暂无入驻申请</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 商户结算 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">商户结算</div>
            <div class="card-s">按账期对账后分账结算 · 待对账 → 已对账 → 已结算</div>
          </div>
          <StatusPill text="T+1 自动分账" tone="blue" />
        </div>
        <div class="overflow-x-auto">
          <table class="tb">
            <thead>
              <tr>
                <th>商户</th>
                <th>周期</th>
                <th>订单数</th>
                <th>金额</th>
                <th>佣金</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in data.settlements" :key="row.id">
                <td class="text-[12.5px] font-medium">{{ row.merchant }}</td>
                <td class="num text-[12px]">{{ row.period }}</td>
                <td class="num text-[12px]">{{ row.orders }}</td>
                <td class="num text-[12.5px] font-semibold text-navy">{{ row.amount }}</td>
                <td class="num text-[12px] text-sub">{{ row.commission }}</td>
                <td><StatusPill :text="row.status" :tone="row.statusTone || 'slate'" /></td>
                <td class="text-right">
                  <button
                    v-if="row.status === '待对账'"
                    class="btn btn-ghost btn-sm !px-2 !py-1"
                    :disabled="store.acting"
                    @click="reconcile(row)"
                  >
                    <i class="fa-solid fa-clipboard-check"></i>对账
                  </button>
                  <button
                    v-else-if="row.status === '已对账'"
                    class="btn btn-primary btn-sm !px-2 !py-1"
                    :disabled="store.acting"
                    @click="settle(row)"
                  >
                    <i class="fa-solid fa-money-check-dollar"></i>结算
                  </button>
                  <span v-else class="text-[11px] text-slate-300">—</span>
                </td>
              </tr>
              <tr v-if="!data.settlements?.length">
                <td colspan="7" class="py-8 text-center text-[12px] text-sub">暂无结算单</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- B 端发票审核 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">B 端发票审核</div>
            <div class="card-s">企业抬头与税号人工复核 · 通过后进入开票队列</div>
          </div>
          <StatusPill text="开票 SLA 48h" tone="amber" />
        </div>
        <div class="overflow-x-auto">
          <table class="tb">
            <thead>
              <tr>
                <th>申请号</th>
                <th>申请人</th>
                <th>抬头</th>
                <th>税号</th>
                <th>金额</th>
                <th>状态</th>
                <th>申请时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in data.invoices" :key="row.id">
                <td class="num text-[12px] font-medium">{{ row.applyNo }}</td>
                <td class="text-[12px]">{{ row.applicant }}</td>
                <td class="text-[12px]">{{ row.title }}</td>
                <td class="num text-[11.5px] text-sub">{{ row.taxNo }}</td>
                <td class="num text-[12.5px] font-semibold text-navy">{{ row.amount }}</td>
                <td><StatusPill :text="row.status" :tone="row.statusTone || 'slate'" /></td>
                <td class="num text-[11.5px] text-sub">{{ row.applied }}</td>
                <td class="whitespace-nowrap text-right">
                  <template v-if="row.status === '待审核'">
                    <button
                      class="btn btn-ghost btn-sm !px-2 !py-1"
                      :disabled="store.acting"
                      @click="approveInvoice(row)"
                    >
                      <i class="fa-solid fa-check text-emerald-600"></i>通过
                    </button>
                    <button
                      class="btn btn-ghost btn-sm !px-2 !py-1"
                      :disabled="store.acting"
                      @click="rejectInvoice(row)"
                    >
                      <i class="fa-solid fa-ban text-rose-500"></i>驳回
                    </button>
                  </template>
                  <span v-else class="text-[11px] text-slate-300">—</span>
                </td>
              </tr>
              <tr v-if="!data.invoices?.length">
                <td colspan="8" class="py-8 text-center text-[12px] text-sub">暂无发票申请</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <div v-else class="grid h-64 place-items-center text-[13px] text-sub">
      <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载财务数据…
    </div>

    <ConfirmDialog v-bind="confirmState" @resolve="resolveConfirm" />
  </div>
</template>
