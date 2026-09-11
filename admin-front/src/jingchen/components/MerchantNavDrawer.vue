<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

/**
 * 商户端导航抽屉（jingchen 模块）：点顶栏汉堡按钮从左侧滑出。
 *
 * 商户没有后台管理侧栏（AdminLayout 按角色隐藏），此前只能靠工作台首页卡片跳转；
 * 抽屉把全部商户页面收进一个可从任意页面呼出的导航，当前页高亮。
 */
defineProps({
  open: { type: Boolean, default: false },
})

const emit = defineEmits(['close'])

const route = useRoute()
const router = useRouter()

const items = [
  { name: 'merchant', label: '商户工作台', icon: 'fa-house', desc: '九大模块总览入口' },
  { name: 'merchantOverview', label: '经营概览', icon: 'fa-chart-line', desc: '曝光 / 消耗 / GMV' },
  { name: 'merchantPlans', label: '投放计划', icon: 'fa-bullhorn', desc: '计划增删改查与暂停恢复' },
  { name: 'merchantOrders', label: '订单与结算', icon: 'fa-file-invoice-dollar', desc: '渠道成交与结算流转' },
  { name: 'merchantMaterials', label: '素材管理', icon: 'fa-photo-film', desc: '素材上传与 A/B 分流' },
  { name: 'merchantVideos', label: '视频接入', icon: 'fa-film', desc: '视频档案与字幕管理' },
  { name: 'merchantVideoStats', label: '播放统计', icon: 'fa-chart-simple', desc: '趋势 / 完播率 / 地域' },
  { name: 'merchantPromo', label: '推广与销售', icon: 'fa-share-nodes', desc: '渠道链接与销售报表' },
  { name: 'merchantGoods', label: '商品管理', icon: 'fa-box-open', desc: '上下架与折扣定价' },
  { name: 'merchantFunds', label: '账户与资金', icon: 'fa-wallet', desc: '余额 / 充值提现 / 流水' },
  { name: 'merchantOnboard', label: '入驻管理', icon: 'fa-building-shield', desc: '资质提交与合同签署' },
]

const currentName = computed(() => String(route.name || ''))

const go = (item) => {
  emit('close')
  if (currentName.value !== item.name) {
    router.push({ name: item.name })
  }
}
</script>

<template>
  <Teleport to="body">
    <transition name="merchant-drawer">
      <div
        v-if="open"
        class="fixed inset-0 z-[60] flex bg-ink/30 backdrop-blur-sm"
        @click.self="emit('close')"
      >
        <aside class="flex h-full w-[264px] flex-col border-r border-line bg-white shadow-lift">
          <div class="flex items-center justify-between border-b border-line px-4 py-3.5">
            <div>
              <div class="text-[13.5px] font-bold text-ink">商户工作台</div>
              <div class="mt-0.5 text-[10.5px] text-sub">页面导航</div>
            </div>
            <button
              class="grid h-7 w-7 place-items-center rounded-md text-sub transition hover:bg-slate-50 hover:text-ink"
              title="收起"
              @click="emit('close')"
            >
              <i class="fa-solid fa-xmark text-[12px]"></i>
            </button>
          </div>

          <nav class="flex-1 space-y-0.5 overflow-y-auto px-2 py-3">
            <button
              v-for="item in items"
              :key="item.name"
              class="flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-left transition"
              :class="currentName === item.name ? 'bg-brand-50 text-electric' : 'text-ink hover:bg-slate-50'"
              @click="go(item)"
            >
              <span
                class="grid h-7 w-7 flex-none place-items-center rounded-md text-[11px]"
                :class="currentName === item.name ? 'bg-electric text-white' : 'bg-[#F1F5F9] text-slate-500'"
              >
                <i class="fa-solid" :class="item.icon"></i>
              </span>
              <span class="min-w-0">
                <span class="block text-[12.5px] font-semibold leading-tight">{{ item.label }}</span>
                <span class="block truncate text-[10px] leading-tight text-sub">{{ item.desc }}</span>
              </span>
              <i
                v-if="currentName === item.name"
                class="fa-solid fa-location-dot ml-auto text-[10px]"
              ></i>
            </button>
          </nav>

          <div class="border-t border-line px-4 py-2.5 text-[10.5px] text-slate-400">
            视界译 VisionTrans · 商户端
          </div>
        </aside>
      </div>
    </transition>
  </Teleport>
</template>

<style scoped>
.merchant-drawer-enter-active,
.merchant-drawer-leave-active {
  transition: opacity 0.22s ease;
}
.merchant-drawer-enter-active :deep(aside),
.merchant-drawer-leave-active :deep(aside) {
  transition: transform 0.26s cubic-bezier(0.16, 1, 0.3, 1);
}
.merchant-drawer-enter-from,
.merchant-drawer-leave-to {
  opacity: 0;
}
.merchant-drawer-enter-from :deep(aside),
.merchant-drawer-leave-to :deep(aside) {
  transform: translateX(-100%);
}
</style>
