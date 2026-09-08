<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import ToastHost from '@/components/ToastHost.vue'

const route = useRoute()
/** 公开页（登录页）不套后台布局，全屏独立渲染 */
const isPublic = computed(() => !!route.meta.public)

/**
 * 首次导航（含 beforeEach 中的令牌校验）未确认前，currentRoute 仍是 START_LOCATION：
 * matched 为空、meta 里没有 public 标记。若此时按 !isPublic 渲染 AdminLayout，
 * TopBar 会立刻请求后台接口，令牌无效时弹出「未登录或登录已过期」，
 * 随后才被守卫重定向到登录页 —— 表现为"还没登录就报错"。
 * 等导航确认（matched 有记录）后再渲染，即可彻底避免。
 */
const ready = computed(() => route.matched.length > 0)
</script>

<template>
  <template v-if="ready">
    <router-view v-if="isPublic" />
    <AdminLayout v-else />
  </template>
  <!-- 全局 Toast：挂载在根组件，登录页与后台页共用，保证所有提示可见 -->
  <ToastHost />
</template>
