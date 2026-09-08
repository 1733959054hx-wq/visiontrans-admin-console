<script setup>
import { onMounted } from 'vue'

import MerchantWelcomeCard from '@/jingchen/components/MerchantWelcomeCard.vue'
import { useMerchantStore } from '@/jingchen/stores/merchant'

/**
 * 商户工作台首页（占位页）。
 *
 * 按需求：商户登录后除顶部导航栏外，内容区暂时为空页面，
 * 这里只提供「欢迎模块」占位；业务模块由前端组员填充，接口由后端组员补充。
 */
const store = useMerchantStore()

onMounted(() => {
  if (!store.home) store.loadHome()
})
</script>

<template>
  <div class="p-5">
    <!-- 欢迎模块（占位） -->
    <MerchantWelcomeCard :home="store.home" :loading="store.loading" />

    <!-- 说明 / 待填充区域 -->
    <div class="card anim mt-4">
      <div class="card-h">
        <div>
          <div class="card-t">工作台说明</div>
          <div class="card-s">当前为占位内容，后续按模块逐步填充</div>
        </div>
      </div>
      <div class="card-b">
        <div v-if="store.loading && !store.home" class="py-6 text-center text-[12.5px] text-sub">
          <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载中…
        </div>
        <ul v-else class="space-y-2">
          <li
            v-for="(item, i) in store.home?.notices || []"
            :key="i"
            class="flex items-start gap-2 text-[12.5px] leading-relaxed text-sub"
          >
            <i class="fa-solid fa-circle-check mt-0.5 text-[11px] text-emerald-500"></i>
            <span>{{ item }}</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- 业务模块占位（前端组员在此填充） -->
    <div class="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
      <div
        v-for="slot in ['经营概览', '订单与结算', '素材与投放']"
        :key="slot"
        class="card anim grid h-40 place-items-center border-dashed bg-slate-50/60"
      >
        <div class="text-center">
          <i class="fa-solid fa-cube text-[18px] text-slate-300"></i>
          <div class="mt-2 text-[13px] font-medium text-slate-400">{{ slot }}</div>
          <div class="mt-0.5 text-[11px] text-slate-400">模块占位 · 待填充</div>
        </div>
      </div>
    </div>
  </div>
</template>
