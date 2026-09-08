<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import ToastHost from '@/components/ToastHost.vue'

const route = useRoute()
/** 公开页（登录页）不套后台布局，全屏独立渲染 */
const isPublic = computed(() => !!route.meta.public)
</script>

<template>
  <router-view v-if="isPublic" />
  <AdminLayout v-else />
  <!-- 全局 Toast：挂载在根组件，登录页与后台页共用，保证所有提示可见 -->
  <ToastHost />
</template>
