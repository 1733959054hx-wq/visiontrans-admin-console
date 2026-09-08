<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusPill from '@/components/StatusPill.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useMerchantStore } from '@/jingchen/stores/merchant'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()

/**
 * 商户推广与销售报表（jingchen 模块业务页）。
 *
 * 「推广渠道」页签:渠道 CRUD + 佣金比例 + 渠道码 / 短链 / 二维码预览;
 * 「销售报表」页签:指标卡 + 近 7 日 GMV + 渠道分布(由订单表实时聚合)。
 */
const store = useMerchantStore()
const { confirmState, askConfirm, resolveConfirm } = useConfirm()

const tab = ref('channels')
const channels = computed(() => store.channels || [])
const sales = computed(() => store.sales)

const fmtMoney = (n) => `¥ ${Number(n || 0).toLocaleString('zh-CN')}`
const fmtCount = (n) => Number(n || 0).toLocaleString('zh-CN')

/* ---- 确定性伪二维码(纯 SVG,渠道码驱动) ---- */
const makeQr = (code) => {
  let h = 2166136261
  for (let i = 0; i < code.length; i++) {
    h ^= code.charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  const rnd = () => {
    h ^= h << 13
    h ^= h >>> 17
    h ^= h << 5
    return ((h >>> 0) / 4294967296)
  }
  const n = 21
  const cells = []
  const finder = (x, y) => {
    for (let a = 0; a < 7; a++) for (let b = 0; b < 7; b++) {
      cells.push(`${(x + a) * 4},${(y + b) * 4},${(a === 0 || a === 6 || b === 0 || b === 6) || (a >= 2 && a <= 4 && b >= 2 && b <= 4) ? '#115E59' : '#fff'}`)
    }
  }
  const inF = (x, y) => (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)
  for (let y = 0; y < n; y++) for (let x = 0; x < n; x++) {
    if (inF(x, y)) continue
    cells.push(`${x * 4},${y * 4},${rnd() > 0.55 ? '#115E59' : '#fff'}`)
  }
  finder(0, 0)
  finder(n - 7, 0)
  finder(0, n - 7)
  return cells.map((c) => `<rect x="${c.split(',')[0]}" y="${c.split(',')[1]}" width="3.4" height="3.4" fill="${c.split(',')[2]}"/>`).join('')
}

const preview = computed(() => channels.value[0] || null)
const shortLink = (ch) => `vt.ms/p/${ch.channelCode}`

const copyLink = async (ch) => {
  try {
    await navigator.clipboard.writeText(`https://${shortLink(ch)}`)
    ui.success('推广短链已复制')
  } catch {
    ui.error('复制失败，请手动复制')
  }
}

/* ---- 渠道 CRUD ---- */
const CHANNEL_FIELDS = [
  { key: 'channelName', label: '渠道名称', type: 'text', required: true, placeholder: '如：抖音内容号' },
  { key: 'ratioPercent', label: '佣金比例', type: 'number', min: 5, max: 50 },
  { key: 'clickCount', label: '累计点击', type: 'number', min: 0 },
  { key: 'dealCount', label: '累计成交', type: 'number', min: 0 },
]
const dialog = reactive({ open: false, title: '', record: {}, mode: 'create' })
const openCreate = () => {
  dialog.open = true
  dialog.mode = 'create'
  dialog.title = '生成新推广渠道'
  dialog.record = { channelName: '', ratioPercent: 25, clickCount: 0, dealCount: 0 }
}
const openEdit = (row) => {
  dialog.open = true
  dialog.mode = 'edit'
  dialog.title = `编辑渠道 · ${row.channelName}`
  dialog.record = { ...row }
}
const submitDialog = async (form) => {
  const mode = dialog.mode
  dialog.open = false
  const payload = {
    channelName: form.channelName,
    ratioPercent: Number(form.ratioPercent) || 25,
    clickCount: Number(form.clickCount) || 0,
    dealCount: Number(form.dealCount) || 0,
  }
  try {
    if (mode === 'create') await store.createChannel(payload)
    else await store.updateChannel(dialog.record.id, payload)
  } catch {
    /* 提示已在 store 统一处理 */
  }
}
const removeChannel = async (row) => {
  if (await askConfirm(`确定删除渠道「${row.channelName}」？其推广链接将失效。`, '删除渠道')) {
    await store.deleteChannel(row.id).catch(() => {})
  }
}

/* ---- 销售报表展示 ---- */
const salesKpis = computed(() => (sales.value?.kpis || []).map((k, i) => ({
  label: k.label,
  value: i === 0 || i === 1 ? fmtMoney(k.value) : `${Number(k.value).toLocaleString('zh-CN')}`,
  icon: ['fa-sack-dollar', 'fa-hand-holding-dollar', 'fa-cart-shopping', 'fa-share-nodes'][i],
})))
const gmvMax = computed(() => Math.max(1, ...(sales.value?.gmv || [])))

onMounted(() => {
  if (!store.channels) store.loadChannels()
  if (!store.sales) store.loadSales()
})
</script>

<template>
  <div class="p-5">
    <PageHeader title="推广与销售报表" desc="渠道推广链接与佣金 · 销售数据实时聚合">
      <template #actions>
        <button class="btn btn-sm" :class="tab === 'channels' ? 'btn-primary' : 'btn-ghost'" @click="tab = 'channels'">推广渠道</button>
        <button class="btn btn-sm" :class="tab === 'sales' ? 'btn-primary' : 'btn-ghost'" @click="tab = 'sales'">销售报表</button>
      </template>
    </PageHeader>

    <!-- ============ 推广渠道 ============ -->
    <template v-if="tab === 'channels'">
      <div class="mt-4 grid grid-cols-1 gap-4 xl:grid-cols-3">
        <!-- 渠道表 -->
        <div class="card anim xl:col-span-2">
          <div class="card-h">
            <div>
              <div class="card-t">推广渠道列表</div>
              <div class="card-s">共 {{ channels.length }} 个渠道 · 佣金比例 5% - 50%</div>
            </div>
            <button class="btn btn-primary btn-sm" @click="openCreate">
              <i class="fa-solid fa-plus"></i>新渠道
            </button>
          </div>
          <div class="overflow-x-auto">
            <table class="tb">
              <thead>
                <tr>
                  <th>渠道 / 渠道码</th>
                  <th class="text-right">佣金</th>
                  <th class="text-right">点击</th>
                  <th class="text-right">成交</th>
                  <th class="text-right">待结算佣金</th>
                  <th class="text-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="store.channelsLoading">
                  <td colspan="6" class="py-8 text-center text-[12px] text-sub">
                    <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载渠道…
                  </td>
                </tr>
                <tr v-for="c in channels" v-else :key="c.id">
                  <td>
                    <div class="text-[12.5px] font-medium text-ink">{{ c.channelName }}</div>
                    <div class="num text-[10.5px] text-sub">{{ c.channelCode }}</div>
                  </td>
                  <td class="num text-right text-[12px]">{{ c.ratioPercent }}%</td>
                  <td class="num text-right text-[12px]">{{ fmtCount(c.clickCount) }}</td>
                  <td class="num text-right text-[12px]">{{ fmtCount(c.dealCount) }}</td>
                  <td class="num text-right text-[12px]">{{ fmtMoney(c.commissionAmount) }}</td>
                  <td>
                    <div class="flex items-center justify-end gap-1">
                      <button class="btn btn-ghost btn-sm !px-2 !py-1" title="编辑" @click="openEdit(c)">
                        <i class="fa-solid fa-pen"></i>
                      </button>
                      <button class="btn btn-ghost btn-sm !px-2 !py-1" title="删除" @click="removeChannel(c)">
                        <i class="fa-solid fa-trash text-rose-500"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 二维码预览 -->
        <div class="card anim">
          <div class="card-h">
            <div>
              <div class="card-t">渠道二维码 / 短链</div>
              <div class="card-s">演示预览 · 随渠道码实时生成</div>
            </div>
          </div>
          <div class="card-b text-center" v-if="preview">
            <div class="mx-auto w-40 overflow-hidden rounded-xl border border-line bg-white p-2">
              <svg viewBox="-4 -4 92 92" class="block h-auto w-full">
                <rect x="-4" y="-4" width="92" height="92" rx="4" fill="#fff" />
                <g v-html="makeQr(preview.channelCode)"></g>
              </svg>
            </div>
            <div class="mt-2 text-[13px] font-bold text-ink">{{ preview.channelName }}</div>
            <div class="num mt-0.5 text-[11px] text-electric">{{ shortLink(preview) }}</div>
            <button class="btn btn-ghost btn-sm mt-3 w-full" @click="copyLink(preview)">
              <i class="fa-solid fa-copy"></i>复制推广短链
            </button>
            <p class="mt-2 text-[10.5px] text-slate-400">佣金 {{ preview.ratioPercent }}% · 扫码直达商品页</p>
          </div>
          <div v-else class="card-b py-10 text-center text-[12px] text-sub">先新建一个推广渠道</div>
        </div>
      </div>
    </template>

    <!-- ============ 销售报表 ============ -->
    <template v-else>
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div v-for="kpi in salesKpis" :key="kpi.label" class="card anim p-4">
          <div class="flex items-center gap-3">
            <span class="grid h-9 w-9 place-items-center rounded-xl bg-[#EFF6FF] text-electric">
              <i class="fa-solid" :class="kpi.icon"></i>
            </span>
            <div>
              <div class="text-[11px] text-sub">{{ kpi.label }}</div>
              <div class="num text-[19px] font-bold text-navy">{{ kpi.value }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="mt-4 grid grid-cols-1 gap-4 xl:grid-cols-2">
        <div class="card anim">
          <div class="card-h">
            <div>
              <div class="card-t">近 7 日 GMV 趋势</div>
              <div class="card-s">来自经营日统计(元)</div>
            </div>
          </div>
          <div class="card-b">
            <div class="flex h-40 items-end gap-2">
              <div v-for="(d, i) in sales?.days || []" :key="d" class="flex min-w-0 flex-1 flex-col items-center gap-1">
                <div class="w-4 rounded-t-sm" style="background: linear-gradient(180deg, #0D9488, #06B6D4)"
                  :style="`height:${Math.round(((sales.gmv[i] || 0) / gmvMax) * 100)}%`"
                  :title="`${d} GMV ${fmtMoney(sales.gmv[i])}`"></div>
                <div class="num text-[10px] text-sub">{{ d.slice(5) }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="card anim">
          <div class="card-h">
            <div>
              <div class="card-t">渠道成交分布</div>
              <div class="card-s">按订单表渠道字段实时聚合</div>
            </div>
          </div>
          <div class="card-b">
            <div v-for="row in sales?.byChannel || []" :key="row.channel" class="mb-2.5 last:mb-0">
              <div class="mb-1 flex items-center justify-between text-[12px]">
                <span class="font-medium text-ink">{{ row.channel }}</span>
                <span class="num text-sub">{{ row.deals }} 笔 · {{ fmtMoney(row.amount) }}</span>
              </div>
              <div class="bar"><i :style="`width:${Math.min(100, Math.round((row.amount / Math.max(...(sales?.byChannel || []).map((r) => r.amount), 1)) * 100))}%`"></i></div>
              <div class="mt-0.5 text-[10.5px] text-sub">佣金支出 {{ fmtMoney(row.commission) }}</div>
            </div>
            <div v-if="!(sales?.byChannel || []).length" class="py-6 text-center text-[12px] text-sub">暂无成交数据</div>
          </div>
        </div>
      </div>
    </template>

    <CrudDialog
      :open="dialog.open"
      :title="dialog.title"
      :fields="CHANNEL_FIELDS"
      :model-value="dialog.record"
      :loading="store.acting"
      @close="dialog.open = false"
      @submit="submitDialog"
    />
    <ConfirmDialog v-bind="confirmState" @resolve="resolveConfirm" />
  </div>
</template>
