<script setup>
import { computed, onMounted, reactive } from 'vue'

import PageHeader from '@/components/PageHeader.vue'
import StatusPill from '@/components/StatusPill.vue'
import { useMerchantStore } from '@/jingchen/stores/merchant'

/**
 * 商户入驻管理（jingchen 模块业务页,功能表「入驻页」）。
 * 三步流程:① 资质提交 → ② 合同签署 → ③ 平台审核(通过 / 驳回);
 * 状态机按后端 merchant_onboarding 表驱动。
 */
const store = useMerchantStore()

const record = computed(() => store.onboard)
const step = computed(() => {
  if (!record.value) return 1 // 从未提交 → 资质提交
  if (record.value.status === '已驳回') return 1
  if (record.value.contractStatus !== '已签署') return 2 // 已提交未签合同
  return 3 // 等待 / 完成审核
})

const form = reactive({
  merchantName: '贵州慧科未来科技有限公司',
  licenseNo: '',
  contact: '',
  phone: '',
  qualification: '营业执照_business.pdf',
})

const submitForm = async () => {
  try {
    await store.submitOnboarding({ ...form })
  } catch {
    /* 提示已在 store 统一处理 */
  }
}
const doSign = async () => {
  try {
    await store.signContract()
  } catch {
    /* 提示已在 store 统一处理 */
  }
}

onMounted(() => {
  store.loadOnboard()
})
</script>

<template>
  <div class="p-5">
    <PageHeader title="入驻管理" desc="资质提交 → 合同签署 → 平台审核 · 审核动作由平台管理端执行">
      <template #actions>
        <button class="btn btn-ghost btn-sm" @click="store.loadOnboard()">
          <i class="fa-solid fa-rotate"></i>刷新
        </button>
      </template>
    </PageHeader>

    <!-- 步骤指示 -->
    <div class="card anim mb-4">
      <div class="card-b flex items-center gap-2">
        <template v-for="(label, i) in ['资质提交', '合同签署', '平台审核']" :key="label">
          <div class="flex flex-1 items-center gap-2">
            <span
              class="grid h-8 w-8 place-items-center rounded-full text-[12px] font-bold"
              :class="step > i + 1 ? 'bg-emerald-500 text-white' : step === i + 1 ? 'bg-electric text-white' : 'bg-slate-100 text-slate-400'"
            >
              {{ step > i + 1 ? '✓' : i + 1 }}
            </span>
            <span class="text-[12.5px] font-semibold" :class="step >= i + 1 ? 'text-ink' : 'text-sub'">{{ label }}</span>
          </div>
          <div v-if="i < 2" class="mx-1 h-0.5 flex-1 rounded" :class="step > i + 1 ? 'bg-emerald-400' : 'bg-line'"></div>
        </template>
      </div>
    </div>

    <!-- 步骤1:资质提交(无申请单 或 已驳回) -->
    <template v-if="step === 1">
      <div class="card anim">
        <div class="card-h">
          <div>
            <div class="card-t">提交入驻资质</div>
            <div class="card-s">提交营业执照等资质文件申请入驻,平台将在 2 小时内完成审核</div>
          </div>
          <StatusPill v-if="record?.status === '已驳回'" text="已驳回 · 重新提交" tone="red" />
        </div>
        <div class="card-b">
          <div v-if="record?.status === '已驳回'" class="mb-3 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-[12px] text-rose-600">
            <i class="fa-solid fa-circle-exclamation mr-1"></i>驳回原因：{{ record.remark || '资质材料不符合要求' }}
          </div>
          <div class="grid grid-cols-1 gap-x-4 gap-y-3.5 md:grid-cols-2">
            <div>
              <label class="lbl">商户名称<span class="text-rose-500">*</span></label>
              <input v-model="form.merchantName" class="field" placeholder="与营业执照一致" />
            </div>
            <div>
              <label class="lbl">营业执照号 / 统一社会信用代码<span class="text-rose-500">*</span></label>
              <input v-model="form.licenseNo" class="field" placeholder="18 位统一社会信用代码" />
            </div>
            <div>
              <label class="lbl">联系人</label>
              <input v-model="form.contact" class="field" placeholder="法定代表人或经办人" />
            </div>
            <div>
              <label class="lbl">联系电话</label>
              <input v-model="form.phone" class="field" placeholder="手机号" />
            </div>
            <div class="md:col-span-2">
              <label class="lbl">资质文件</label>
              <div class="rounded-lg border-2 border-dashed border-blue-200 bg-blue-50/50 px-4 py-5 text-center cursor-pointer hover:bg-blue-50/80 transition"
                @click="form.qualification = '营业执照_' + Date.now() + '.pdf'">
                <i class="fa-solid fa-cloud-arrow-up text-[22px] text-electric"></i>
                <div class="mt-1.5 text-[12.5px] font-semibold text-ink">
                  {{ form.qualification || '点击上传营业执照等资质文件(演示)' }}
                </div>
                <div class="mt-0.5 text-[10.5px] text-sub">JPG / PNG / PDF · ≤ 20MB</div>
              </div>
            </div>
          </div>
          <button class="btn btn-primary mt-4 w-full" :disabled="store.acting" @click="submitForm">
            <i class="fa-solid fa-paper-plane"></i>提交资质申请
          </button>
        </div>
      </div>
    </template>

    <!-- 步骤2:合同签署 -->
    <template v-else-if="step === 2">
      <div class="card anim">
        <div class="card-h">
          <div>
            <div class="card-t">签署合作协议</div>
            <div class="card-s">请阅读《视界译平台商户合作协议》并完成签署(H5 页面 / 短信链接同步发送)</div>
          </div>
          <StatusPill text="待签署" tone="amber" />
        </div>
        <div class="card-b">
          <div class="rounded-xl border border-line bg-slate-50 p-4 text-[12px] leading-relaxed text-sub max-h-48 overflow-y-auto">
            <b class="text-ink">一、合作内容</b>：商户入驻视界译 AR 翻译广告与知识包分销平台，享有广告投放、商品分销、视频版权接入能力。<br><br>
            <b class="text-ink">二、结算与费用</b>：广告消耗按 oCPM 实时扣减；分销佣金按订单实付金额的约定比例 T+7 结算；资金存管于央行监管专户。<br><br>
            <b class="text-ink">三、数据与合规</b>：上传素材需符合《广告法》及平台审核规范；运营数据保留 180 天并可导出。<br><br>
            <b class="text-ink">四、协议期限</b>：自签署之日起 12 个月，到期前 30 日提醒年审续签。
          </div>
          <div class="mt-3 flex items-center gap-2 rounded-lg border border-line px-3 py-2.5">
            <i class="fa-solid fa-file-signature text-electric"></i>
            <span class="text-[12.5px]">申请单号：<b class="num">{{ record.applyNo }}</b> · {{ record.merchantName }}</span>
          </div>
          <button class="btn btn-primary mt-4 w-full" :disabled="store.acting" @click="doSign">
            <i class="fa-solid fa-file-signature"></i>签署合作协议
          </button>
        </div>
      </div>
    </template>

    <!-- 步骤3:平台审核 -->
    <template v-else>
      <div class="card anim">
        <div class="card-h">
          <div>
            <div class="card-t">平台审核</div>
            <div class="card-s">资质与合同已提交，等待平台审核结果</div>
          </div>
          <StatusPill :text="record.status" :tone="record.status === '已通过' ? 'green' : record.status === '已驳回' ? 'red' : 'amber'" />
        </div>
        <div class="card-b">
          <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
            <div class="rounded-lg border border-line p-3">
              <div class="text-[10.5px] text-sub">申请单号</div>
              <div class="num mt-1 text-[13px] font-bold text-ink">{{ record.applyNo }}</div>
            </div>
            <div class="rounded-lg border border-line p-3">
              <div class="text-[10.5px] text-sub">提交时间</div>
              <div class="num mt-1 text-[13px] font-bold text-ink">{{ record.submitted }}</div>
            </div>
            <div class="rounded-lg border border-line p-3">
              <div class="text-[10.5px] text-sub">营业执照号</div>
              <div class="num mt-1 text-[13px] font-bold text-ink">{{ record.licenseNo }}</div>
            </div>
            <div class="rounded-lg border border-line p-3">
              <div class="text-[10.5px] text-sub">联系人 / 电话</div>
              <div class="mt-1 text-[13px] font-bold text-ink">{{ record.contact }} · {{ record.phone }}</div>
            </div>
          </div>
          <div class="mt-3 rounded-lg border p-3" :class="record.contractStatus === '已签署' ? 'border-emerald-200 bg-emerald-50/50' : 'border-amber-200 bg-amber-50/50'">
            <span class="text-[12px] font-medium" :class="record.contractStatus === '已签署' ? 'text-emerald-700' : 'text-amber-700'">
              合作协议：{{ record.contractStatus }}
            </span>
          </div>
          <div v-if="record.status === '已驳回'" class="mt-3 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-[12px] text-rose-600">
            <i class="fa-solid fa-circle-exclamation mr-1"></i>驳回原因：{{ record.remark || '资质材料不符合要求' }} · 可在顶部重新提交
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
