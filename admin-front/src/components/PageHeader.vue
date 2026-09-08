<script setup>
defineProps({
  title: { type: String, required: true },
  desc: { type: String, default: '' },
})
</script>

<template>
  <div class="anim mb-4 flex items-end justify-between gap-4">
    <div class="min-w-0">
      <h1 class="text-[19px] font-bold tracking-tight text-ink">{{ title }}</h1>
      <p class="mt-1 text-[12.5px] text-sub">{{ desc }}</p>
    </div>
    <div class="flex flex-none items-center gap-2">
      <slot name="actions"></slot>
    </div>
  </div>
</template>
