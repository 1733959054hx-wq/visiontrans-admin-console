<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

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

/* ---- 账户信息维护 ---- */
const profile = computed(() => store.profile)
const profileOpen = ref(false)
const profileForm = reactive({ contact: '', phone: '', settleAccount: '' })

const openProfileEdit = () => {
  profileForm.contact = profile.value?.contact || ''
  profileForm.phone = profile.value?.phone || ''
  profileForm.settleAccount = profile.value?.settleAccount || ''
  profileOpen.value = true
}
const saveProfile = async () => {
  if (!profileForm.contact.trim() || !profileForm.phone.trim()) {
    ui.error('联系人与联系电话不能为空')
    return
  }
  try {
    await store.saveProfile({
      contact: profileForm.contact,
      phone: profileForm.phone,
      settleAccount: profileForm.settleAccount || null,
    })
    profileOpen.value = false
  } catch {
    /* 提示已在 store 统一处理 */
  }
}

onMounted(() => {
  if (!store.funds) store.loadFunds()
  store.loadProfile()
})
</script>

<template>
  <div class="p-5">
    <PageHeader title="账户与资金" desc="资金账户总览 · 在线充值 / 提现申请 / 资金流水明细">
      <template #actions>
        <button class="btn btn-primary btn-sm" @click="openProfileEdit">
          <i class="fa-solid fa-user-pen"></i>基本信息维护
        </button>
        <button class="btn btn-ghost btn-sm" @click="store.loadFunds()">
          <i class="fa-solid fa-rotate"></i>刷新
        </button>
      </template>
    </PageHeader>

    <!-- 账户信息卡 -->
    <div class="card anim mb-1 px-4 py-2.5">
      <div class="flex flex-wrap items-center gap-x-6 gap-y-1 text-[12px]">
        <span class="font-bold text-ink"><i class="fa-solid fa-building-columns mr-1.5 text-teal-500"></i>账户信息</span>
        <span class="text-sub">商户 <b class="text-ink">{{ profile?.merchantName || '—' }}</b></span>
        <span class="text-sub">联系人 <b class="text-ink">{{ profile?.contact || '—' }}</b></span>
        <span class="text-sub">电话 <b class="num text-ink">{{ profile?.phone || '—' }}</b></span>
        <span class="text-sub">结算账户 <b class="num text-ink">{{ profile?.settleAccount || '未设置' }}</b></span>
      </div>
    </div>

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

    <!-- 资料编辑弹窗 -->
    <div v-if="profileOpen" class="fixed inset-0 z-[70] flex items-start justify-center overflow-y-auto bg-ink/30 p-6 backdrop-blur-sm">
      <div class="w-full max-w-md rounded-2xl bg-white shadow-lift">
        <div class="flex items-center justify-between border-b border-[#EEF2F7] px-5 py-3.5">
          <div class="text-[14.5px] font-semibold text-ink">基本信息维护</div>
          <button class="text-sub transition hover:text-ink" @click="profileOpen = false">
            <i class="fa-solid fa-xmark"></i>
          </button>
        </div>
        <div class="space-y-3.5 px-5 py-4">
          <div>
            <label class="lbl">商户名称(展示)</label>
            <input class="field" :value="profile?.merchantName" disabled />
          </div>
          <div>
            <label class="lbl">联系人<span class="text-rose-500">*</span></label>
            <input v-model="profileForm.contact" class="field" placeholder="法定代表人或经办人" />
          </div>
          <div>
            <label class="lbl">联系电话<span class="text-rose-500">*</span></label>
            <input v-model="profileForm.phone" class="field" placeholder="手机号" />
          </div>
          <div>
            <label class="lbl">结算账户</label>
            <input v-model="profileForm.settleAccount" class="field" placeholder="如：工行(****8821)" />
          </div>
        </div>
        <div class="flex items-center justify-end gap-2 border-t border-[#EEF2F7] px-5 py-3.5">
          <button class="btn btn-ghost btn-sm" @click="profileOpen = false">取消</button>
          <button class="btn btn-primary btn-sm" :disabled="store.acting" @click="saveProfile">
            <i v-if="store.acting" class="fa-solid fa-circle-notch fa-spin"></i>保存修改
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
