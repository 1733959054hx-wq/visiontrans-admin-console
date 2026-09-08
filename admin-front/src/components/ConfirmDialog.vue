<script setup>
defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '操作确认' },
  message: { type: String, default: '' },
  /** 危险操作：确认按钮改为红色警示态 */
  danger: { type: Boolean, default: false },
  /** 确认按钮文案 */
  confirmText: { type: String, default: '确定' },
  /** 操作主体身份卡（如 { avatar, name, role }），醒目展示"是谁" */
  subject: { type: Object, default: null },
})

const emit = defineEmits(['resolve'])
</script>

<template>
  <div
    v-if="open"
    class="fixed inset-0 z-[70] grid place-items-center bg-ink/30 p-4 backdrop-blur-sm"
    @click.self="emit('resolve', false)"
  >
    <div class="w-full max-w-sm rounded-2xl bg-white p-5 shadow-lift">
      <div class="flex items-start gap-3">
        <span class="grid h-9 w-9 flex-none place-items-center rounded-xl bg-rose-50 text-rose-500">
          <i class="fa-solid fa-circle-exclamation"></i>
        </span>
        <div class="min-w-0">
          <div class="text-[14.5px] font-semibold text-ink">{{ title }}</div>
          <div class="mt-1 text-[12.5px] leading-relaxed text-sub">{{ message }}</div>
        </div>
      </div>
      <!-- 操作主体身份卡：一眼看清是谁在执行此操作 -->
      <div
        v-if="subject"
        class="mt-3 flex items-center gap-3 rounded-xl border border-line bg-slate-50 px-3 py-2.5"
      >
        <div
          class="grid h-9 w-9 flex-none place-items-center rounded-full text-[13px] font-semibold text-white"
          style="background: linear-gradient(135deg, #1E3A8A, #0EA5E9)"
        >
          {{ subject.avatar || String(subject.name || '?').slice(0, 1) }}
        </div>
        <div class="min-w-0">
          <div class="truncate text-[13px] font-semibold text-ink">{{ subject.name }}</div>
          <div class="truncate text-[11px] text-sub">{{ subject.role }}</div>
        </div>
        <span class="pill pill-slate ml-auto flex-none">当前登录</span>
      </div>

      <div class="mt-5 flex justify-end gap-2">
        <button class="btn btn-ghost btn-sm" @click="emit('resolve', false)">取消</button>
        <button
          v-if="danger"
          class="btn btn-sm text-white"
          style="background: linear-gradient(135deg, #be123c, #e11d48); box-shadow: 0 8px 20px -10px rgba(225, 29, 72, 0.9)"
          @click="emit('resolve', true)"
        >
          {{ confirmText }}
        </button>
        <button v-else class="btn btn-primary btn-sm" @click="emit('resolve', true)">{{ confirmText }}</button>
      </div>
    </div>
  </div>
</template>
