<script setup>
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()
</script>

<template>
  <div class="pointer-events-none fixed right-5 top-5 z-[80] flex w-72 flex-col gap-2">
    <transition-group name="toast">
      <div
        v-for="item in ui.toasts"
        :key="item.id"
        class="pointer-events-auto flex items-start gap-2 rounded-xl border px-3.5 py-2.5 text-[12.5px] shadow-lift backdrop-blur"
        :class="
          item.type === 'error'
            ? 'border-rose-100 bg-rose-50/95 text-rose-700'
            : 'border-emerald-100 bg-emerald-50/95 text-emerald-700'
        "
      >
        <i class="fa-solid mt-0.5 text-[11px]" :class="item.type === 'error' ? 'fa-circle-exclamation' : 'fa-circle-check'"></i>
        <span class="leading-snug">{{ item.message }}</span>
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
