<script setup>
import { computed } from 'vue'

import { useAuthStore } from '@/stores/auth'

/**
 * 商户工作台欢迎模块（占位）。
 * 数据来自本模块 store；顶部导航已展示身份，这里仍从 auth 取以免重复请求。
 */
const props = defineProps({
  home: { type: Object, default: null },
  loading: { type: Boolean, default: false },
})

const auth = useAuthStore()

const title = computed(() => props.home?.title || '商户工作台')
const welcome = computed(
  () => props.home?.welcome || `欢迎回来，${props.home?.merchantName || auth.user?.name || '商户'}！`,
)
const roleName = computed(() => props.home?.roleName || auth.user?.role || '商户用户')
</script>

<template>
  <div class="card anim overflow-hidden">
    <div
      class="relative px-6 py-7"
      style="background: linear-gradient(120deg, #0b1e4d 0%, #1e3a8a 55%, #0ea5e9 100%)"
    >
      <div class="relative z-10">
        <div class="text-[11.5px] font-semibold uppercase tracking-[0.18em] text-sky-200/80">
          MERCHANT WORKSPACE
        </div>
        <h1 class="mt-2 text-[22px] font-bold tracking-tight text-white">
          <i v-if="loading" class="fa-solid fa-circle-notch fa-spin mr-2 text-[16px]"></i>{{ title }}
        </h1>
        <p class="mt-1.5 text-[13px] text-sky-100/85">{{ welcome }}</p>
        <div class="mt-3 flex flex-wrap items-center gap-2">
          <span class="pill pill-blue">{{ roleName }}</span>
          <span class="pill pill-slate">{{ home?.merchantCode || auth.user?.group || '—' }}</span>
          <span v-if="home?.contact" class="pill pill-slate">
            <i class="fa-regular fa-user mr-1"></i>{{ home.contact }}
          </span>
        </div>
      </div>
      <i class="fa-solid fa-store absolute -right-2 bottom-[-18px] text-[92px] text-white/10"></i>
    </div>
  </div>
</template>
