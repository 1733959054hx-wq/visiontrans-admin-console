<script setup>
import { useRoute } from 'vue-router'

import BrandMark from '@/components/BrandMark.vue'
import { useAppStore } from '@/stores/app'

const app = useAppStore()
const route = useRoute()
</script>

<template>
  <aside
    v-if="!app.sideCollapsed"
    class="sidebar-bg relative z-20 flex w-[248px] flex-none flex-col overflow-hidden text-ink"
  >
    <!-- 品牌区 -->
    <div class="relative z-10 flex items-center gap-3 px-5 py-5">
      <div
        class="grid h-10 w-10 place-items-center rounded-xl text-base text-white"
        style="background: linear-gradient(135deg, #0d9488, #06b6d4); box-shadow: 0 8px 20px -8px rgba(13, 148, 136, 0.9)"
      >
        <BrandMark class="h-6 w-6" />
      </div>
      <div class="leading-tight">
        <div class="text-[15px] font-bold tracking-wide">{{ app.system?.name || '视界译 VisionTrans' }}</div>
        <div class="text-[10.5px] tracking-wider text-sub">{{ app.system?.subtitle || '' }}</div>
      </div>
    </div>

    <!-- 集群状态 -->
    <div class="relative z-10 px-5 pb-3">
      <div class="flex items-center gap-2.5 rounded-xl border border-line bg-slate-50 px-3 py-2.5">
        <span class="dot-live h-2 w-2 rounded-full bg-emerald-500"></span>
        <div class="text-[11.5px] leading-tight">
          <div class="font-semibold">{{ app.system?.clusterStatus || '集群正常' }}</div>
          <div class="text-[10px] text-sub">{{ app.system?.clusterLatency || '' }}</div>
        </div>
      </div>
    </div>

    <!-- 导航 -->
    <div
      class="relative z-10 px-4 pb-1 pt-3 text-[10px] font-semibold uppercase tracking-[0.14em] text-slate-400"
    >
      {{ app.nav.group }}
    </div>
    <nav class="relative z-10 flex-1 space-y-1 overflow-y-auto px-4 py-2">
      <router-link
        v-for="item in app.nav.items"
        :key="item.id"
        :to="{ name: item.id }"
        class="nav-item"
        :class="{ active: route.name === item.id }"
      >
        <span class="nav-ico"><i class="fa-solid" :class="item.icon"></i></span>
        <span class="min-w-0 flex-1 leading-tight">
          <span class="block truncate">{{ item.text }}</span>
          <span class="nav-sub">{{ item.sub }}</span>
        </span>
        <i class="fa-solid fa-chevron-right ml-auto text-[9px] opacity-40"></i>
      </router-link>
    </nav>

    <!-- 当前管理员 -->
    <div class="relative z-10 border-t border-line px-4 py-4">
      <div class="flex items-center gap-3">
        <div
          class="grid h-9 w-9 flex-none place-items-center rounded-full text-[12px] font-bold text-white"
          style="background: linear-gradient(135deg, #0f766e, #06b6d4)"
        >
          {{ app.system?.currentUserAvatar || '管' }}
        </div>
        <div class="min-w-0 flex-1 leading-tight">
          <div class="truncate text-[12.5px] font-semibold">{{ app.system?.currentUser || '—' }}</div>
          <div class="truncate text-[10px] text-sub">ID: {{ app.system?.currentUserId || '—' }}</div>
        </div>
        <i
          class="fa-solid fa-ellipsis cursor-pointer text-xs text-slate-400 transition hover:text-ink"
          title="折叠侧栏"
          @click="app.toggleSide()"
        ></i>
      </div>
    </div>
  </aside>
</template>
