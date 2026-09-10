<script setup>
import { computed, onMounted, reactive } from 'vue'

import PageHeader from '@/components/PageHeader.vue'
import StatusPill from '@/components/StatusPill.vue'
import { useMerchantStore } from '@/jingchen/stores/merchant'
import { useUiStore } from '@/stores/ui'

/**
 * 商户账户与资金（jingchen 模块业务页,账户管理-资金账户 / 资金流水 / 提现管理）。
 * 可用余额实时计算,充值 / 提现直接落库(提现申请后状态审核中)。
 */
const store = useMerchantStore()
const ui = useUiStore()

const funds = computed(() => store.funds)
const flows = computed(() => funds.value?.flows || [])
const withdraws = computed(() => funds.value?.withdraws || [])

const yuan = (n) => `¥ ${Number(n || 0).toLocaleString('zh-CN')}`

const dirMeta = { 收入: { cls: 'in', icon: '↓' }, 支出: { cls: 'out', icon: '↑' } }
const wdTone = { 审核中: 'amber', 已到账: 'green', 已驳回: 'red' }

const form = reactive({ amount: 50000, quick: 50000 })
const QUICK = [10000, 50000, 200000, 500000]

const doRecharge = async () => {
  const amount = Number(form.amount)
  if (!amount || amount <= 0) {
    ui.error('充值金额必须大于 0')
    return
  }
  try {
    await store.recharge(amount)
    form.amount = 50000
  } catch {
    /* 提示已在 store 统一处理 */
  }
}
const doWithdraw = async () => {
  const amount = Number(form.amount)
  if (!amount || amount <= 0) {
    ui.error('提现金额必须大于 0')
    return
  }
  try {
    await store.withdraw(amount)
    form.amount = 50000
  } catch {
    /* 提示已在 store 统一处理 */
  }
}
const applyQuick = (v) => {
  form.quick = v
  form.amount = v
}

onMounted(() => {
  if (!store.funds) store.loadFunds()
})
</script>

<template>
  <div class="p-5">
    <PageHeader title="账户与资金" desc="资金账户总览 · 在线充值 / 提现申请 / 资金流水明细">
      <template #actions>
        <button class="btn btn-ghost btn-sm" @click="store.loadFunds()">
          <i class="fa-solid fa-rotate"></i>刷新
        </button>
      </template>
    </PageHeader>

    <template v-if="funds">
      <!-- 余额卡 -->
      <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
        <div class="card anim overflow-hidden">
          <div class="px-5 py-6 text-white" style="background: linear-gradient(135deg, #0b1e4d 0%, #1e3a8a 60%, #2563eb 100%)">
            <div class="text-[11px] uppercase tracking-[0.2em] text-sky-200/80">可用余额</div>
            <div class="num mt-2 text-[30px] font-bold">{{ yuan(funds.balance) }}</div>
            <div class="mt-1.5 text-[11px] text-sky-100/75">央行存管专户 · 实时清算</div>
          </div>
        </div>
        <div class="card anim p-4">
          <div class="text-[12px] text-sub">累计收入</div>
          <div class="num mt-1.5 text-[24px] font-bold text-emerald-600">{{ yuan(funds.income) }}</div>
          <div class="mt-2 text-[10.5px] text-sub">充值 + 佣金入账 + 退款</div>
        </div>
        <div class="card anim p-4">
          <div class="text-[12px] text-sub">累计支出</div>
          <div class="num mt-1.5 text-[24px] font-bold text-rose-500">{{ yuan(funds.expense) }}</div>
          <div class="mt-2 text-[10.5px] text-sub">广告消耗 + 提现</div>
        </div>
      </div>

      <!-- 充值 / 提现 -->
      <div class="mt-4 grid grid-cols-1 gap-4 xl:grid-cols-2">
        <div class="card anim">
          <div class="card-h"><div><div class="card-t">在线充值</div><div class="card-s">支付宝 / 微信 / 对公转账 · T+0 到账</div></div>
            <span class="pill pill-blue">演示</span></div>
          <div class="card-b">
            <label class="lbl">充值金额(元)</label>
            <input v-model.number="form.amount" type="number" min="0" class="field" />
            <div class="mt-2 flex flex-wrap gap-2">
              <button v-for="q in QUICK" :key="q" class="btn btn-sm" :class="form.quick === q ? 'btn-primary' : 'btn-ghost'" @click="applyQuick(q)">
                {{ Number(q).toLocaleString('zh-CN') }}
              </button>
            </div>
            <button class="btn btn-primary btn-sm mt-3 w-full" :disabled="store.acting" @click="doRecharge">
              <i class="fa-solid fa-credit-card"></i>确认充值
            </button>
          </div>
        </div>
        <div class="card anim">
          <div class="card-h"><div><div class="card-t">提现管理</div><div class="card-s">仅支持企业实名对公账户 · T+1 到账 · 免手续费</div></div></div>
          <div class="card-b">
            <label class="lbl">提现金额(元)</label>
            <input v-model.number="form.amount" type="number" min="0" class="field" />
            <div class="mt-2 flex items-center gap-3 text-[11px] text-sub">
              <span>单日限额 ¥2,000,000</span>
              <span class="num ml-auto">手续费 ¥ 0.00(平台补贴)</span>
            </div>
            <button class="btn btn-primary btn-sm mt-3 w-full" :disabled="store.acting" @click="doWithdraw">
              <i class="fa-solid fa-bolt"></i>提交提现申请
            </button>
          </div>
        </div>
      </div>

      <!-- 资金流水 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">资金流水明细</div>
            <div class="card-s">共 {{ flows.length }} 笔 · 余额快照逐笔可追溯</div>
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="tb">
            <thead>
              <tr>
                <th>类型</th>
                <th>收支</th>
                <th class="text-right">金额</th>
                <th class="text-right">流水后余额</th>
                <th>备注 / 单号</th>
                <th class="num">时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="store.fundsLoading">
                <td colspan="6" class="py-8 text-center text-[12px] text-sub">
                  <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载流水…
                </td>
              </tr>
              <tr v-for="f in flows" v-else :key="f.id">
                <td class="text-[12.5px] font-medium text-ink">{{ f.flowType }}</td>
                <td>
                  <span class="tag" :class="f.direction === '收入' ? 'pill-green' : 'pill-red'">
                    {{ dirMeta[f.direction]?.icon || '' }} {{ f.direction }}
                  </span>
                </td>
                <td class="num text-right text-[12.5px] font-bold" :class="f.direction === '收入' ? 'text-emerald-600' : 'text-rose-500'">
                  {{ f.direction === '收入' ? '+' : '−' }} {{ yuan(f.amount).slice(2) }}
                </td>
                <td class="num text-right text-[12px] text-sub">{{ yuan(f.balanceAfter) }}</td>
                <td class="text-[11.5px] text-sub">{{ f.remark }}</td>
                <td class="num text-right text-[11px] text-sub">{{ f.createdAt }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 提现记录 -->
      <div class="card anim mt-4">
        <div class="card-h">
          <div>
            <div class="card-t">提现记录</div>
            <div class="card-s">共 {{ withdraws.length }} 笔申请</div>
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="tb">
            <thead>
              <tr>
                <th>提现单号</th>
                <th class="text-right">金额</th>
                <th>收款账户</th>
                <th>申请时间</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="w in withdraws" :key="w.id">
                <td class="num text-[12px] font-medium text-ink">{{ w.withdrawNo }}</td>
                <td class="num text-right text-[12.5px] font-bold">{{ yuan(w.amount) }}</td>
                <td class="text-[12px] text-sub">{{ w.account }}</td>
                <td class="num text-[11.5px] text-sub">{{ w.appliedAt }}</td>
                <td><StatusPill :text="w.status" :tone="wdTone[w.status] || 'slate'" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <div v-else class="grid h-64 place-items-center text-[13px] text-sub">
      <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载资金数据…
    </div>
  </div>
</template>
