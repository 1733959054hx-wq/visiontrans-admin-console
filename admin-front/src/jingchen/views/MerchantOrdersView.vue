<script setup>
import { computed, onMounted } from 'vue'

import PageHeader from '@/components/PageHeader.vue'
import StatusPill from '@/components/StatusPill.vue'
import { useConfirm } from '@/composables/useConfirm'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useMerchantStore } from '@/jingchen/stores/merchant'

/**
 * 商户订单与结算（jingchen 模块业务页）。
 *
 * 订单列表 + 「待结算 → 已结算」流转；数据来自 /api/merchant/orders（@RequireRole(MERCHANT)）。
 */
const store = useMerchantStore()
const { confirmState, askConfirm, resolveConfirm } = useConfirm()

const orders = computed(() => store.orders || [])

const fmtMoney = (n) => `¥ ${Number(n || 0).toLocaleString('zh-CN')}`

const statusMeta = (s) =>
  ({ 已结算: { tone: 'green' }, 待结算: { tone: 'amber' }, 退款中: { tone: 'red' } })[s] || { tone: 'slate' }

/** 摘要:总额 / 佣金 / 待结算笔数 */
const summary = computed(() => {
  const list = orders.value
  const sum = (k, status) =>
    list.filter((o) => !status || o.status === status).reduce((s, o) => s + (o[k] || 0), 0)
  return [
    { label: '成交总额', value: fmtMoney(sum('amount')), icon: 'fa-coins', tone: 'text-navy' },
    { label: '分销佣金', value: fmtMoney(sum('commission')), icon: 'fa-hand-holding-dollar', tone: 'text-electric' },
    { label: '待结算笔数', value: `${list.filter((o) => o.status === '待结算').length} 笔`, icon: 'fa-hourglass-half', tone: 'text-amber-600' },
    { label: '退款中', value: `${list.filter((o) => o.status === '退款中').length} 笔`, icon: 'fa-rotate-left', tone: 'text-rose-500' },
  ]
})

const settleOne = async (order) => {
  if (await askConfirm(`确认结算订单「${order.orderNo}」？佣金 ${fmtMoney(order.commission)} 将计入可提现余额。`, '订单结算')) {
    await store.settleOrder(order.id).catch(() => {})
  }
}

onMounted(() => {
  if (!store.orders) store.loadOrders()
})
</script>

<template>
  <div class="p-5">
    <PageHeader title="订单与结算" desc="渠道成交订单一览 · 支持待结算订单一键结算">
      <template #actions>
        <button class="btn btn-ghost btn-sm" @click="store.loadOrders()">
          <i class="fa-solid fa-rotate"></i>刷新
        </button>
      </template>
    </PageHeader>

    <!-- 摘要 -->
    <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
      <div v-for="item in summary" :key="item.label" class="card anim relative overflow-hidden p-4">
        <div class="flex items-start justify-between gap-2">
          <div class="min-w-0">
            <div class="text-[12px] text-sub">{{ item.label }}</div>
            <div class="num mt-1.5 text-[22px] font-bold leading-tight" :class="item.tone">{{ item.value }}</div>
          </div>
          <div class="grid h-10 w-10 flex-none place-items-center rounded-xl bg-[#F1F5F9] text-[15px] text-slate-500">
            <i class="fa-solid" :class="item.icon"></i>
          </div>
        </div>
      </div>
    </div>

    <!-- 订单表 -->
    <div class="card anim mt-4">
      <div class="card-h">
        <div>
          <div class="card-t">成交订单列表</div>
          <div class="card-s">共 {{ orders.length }} 笔 · 结算状态由本页流转维护</div>
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="tb">
          <thead>
            <tr>
              <th>订单号 / 时间</th>
              <th>成交商品</th>
              <th>渠道</th>
              <th class="text-right">成交金额</th>
              <th class="text-right">分销佣金</th>
              <th>状态</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="store.ordersLoading">
              <td colspan="7" class="py-8 text-center text-[12px] text-sub">
                <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>正在加载订单…
              </td>
            </tr>
            <tr v-for="order in orders" v-else :key="order.id">
              <td>
                <div class="num text-[12px] font-medium text-ink">{{ order.orderNo }}</div>
                <div class="num text-[10.5px] text-sub">{{ order.createdAt }}</div>
              </td>
              <td class="text-[12.5px]">{{ order.goodsName }}</td>
              <td><StatusPill :text="order.channel" tone="blue" small /></td>
              <td class="num text-right text-[12px]">{{ fmtMoney(order.amount) }}</td>
              <td class="num text-right text-[12px] text-sub">{{ fmtMoney(order.commission) }}</td>
              <td><StatusPill :text="order.status" :tone="statusMeta(order.status).tone" /></td>
              <td>
                <div class="flex items-center justify-end">
                  <button
                    v-if="order.status === '待结算'"
                    class="btn btn-primary btn-sm !px-3 !py-1.5"
                    :disabled="store.acting"
                    @click="settleOne(order)"
                  >
                    <i class="fa-solid fa-check mr-1"></i>结算
                  </button>
                  <span v-else-if="order.status === '退款中'" class="text-[11.5px] text-rose-400">退款流程中</span>
                  <span v-else class="text-[11.5px] text-slate-300">已完成</span>
                </div>
              </td>
            </tr>
            <tr v-if="!store.ordersLoading && !orders.length">
              <td colspan="7" class="py-8 text-center text-[12px] text-sub">暂无订单记录</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <ConfirmDialog v-bind="confirmState" @resolve="resolveConfirm" />
  </div>
</template>
