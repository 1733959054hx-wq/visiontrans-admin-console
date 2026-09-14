<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import BrandMark from '@/components/BrandMark.vue'
import {
  DEFAULT_EXT_API_BASE,
  getSavedExtensionApiBase,
  normalizeApiBase,
  saveExtensionApiBase,
} from '@/api/extConfig'

/**
 * 扩展首次使用配置页：浏览器扩展无法用相对路径找到后端，
 * 必须先让用户填写可访问的后端地址（如本机 http://localhost:8080 或公网服务器）。
 * 地址仅保存在扩展本地（localStorage），不上报任何第三方。
 */
const router = useRouter()

const url = ref(getSavedExtensionApiBase() || DEFAULT_EXT_API_BASE)
const error = ref('')
const reconfigured = !!getSavedExtensionApiBase()

const submit = () => {
  const normalized = normalizeApiBase(url.value)
  if (!normalized) {
    error.value = '请输入合法的 http(s) 地址，例如 http://localhost:8080'
    return
  }
  saveExtensionApiBase(normalized)
  router.push('/login')
}
</script>

<template>
  <div class="relative flex min-h-screen w-full items-center justify-center bg-titan px-4">
    <div class="w-full max-w-[420px]">
      <div class="relative overflow-hidden rounded-2xl border border-line bg-white p-7 shadow-card">
        <div class="absolute inset-x-0 top-0 h-[3px]" style="background: linear-gradient(90deg, #0f766e, #0d9488 55%, #06b6d4)"></div>

        <div class="mb-5 flex items-center gap-3">
          <div
            class="grid h-11 w-11 place-items-center rounded-xl text-white shadow-lift"
            style="background: linear-gradient(135deg, #0d9488, #06b6d4)"
          >
            <BrandMark class="h-[26px] w-[26px]" />
          </div>
          <div>
            <div class="text-[16px] font-bold tracking-tight text-ink">连接服务器</div>
            <div class="text-[11.5px] text-sub">首次使用扩展，请配置后端服务地址</div>
          </div>
        </div>

        <label class="lbl">后端服务地址</label>
        <div class="relative mb-2">
          <i class="fa-solid fa-server absolute left-3.5 top-1/2 -translate-y-1/2 text-[13px] text-slate-400"></i>
          <input
            v-model="url"
            class="field pl-10"
            placeholder="http://localhost:8080"
            autocomplete="off"
            @keyup.enter="submit"
          />
        </div>

        <div
          v-if="error"
          class="mb-3 flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-[12.5px] font-medium text-rose-600"
        >
          <i class="fa-solid fa-circle-exclamation mt-0.5 shrink-0"></i>
          <span>{{ error }}</span>
        </div>

        <button type="button" class="btn btn-primary w-full justify-center py-2.5 text-[13.5px]" @click="submit">
          <span>保存并前往登录</span>
        </button>

        <div class="mt-4 space-y-1.5 rounded-lg border border-brand-100 bg-brand-50 p-3 text-[11.5px] leading-relaxed text-sub">
          <p>
            <i class="fa-solid fa-circle-info mr-1 text-electric"></i>
            本机开发默认地址 <code class="rounded bg-white/60 px-1 py-0.5 font-mono text-ink">http://localhost:8080</code>，系统会自动补全 <code class="font-mono">/api</code>。
          </p>
          <p>多人使用时，把地址换成可访问的服务器地址即可，无需重新构建扩展。</p>
          <p v-if="reconfigured">当前已保存配置，保存新地址后下次请求立即生效。</p>
        </div>
      </div>

      <div class="mt-5 text-center text-[11px] text-slate-400">
        视界译 VisionTrans · 浏览器扩展 · 地址仅存储于本地
      </div>
    </div>
  </div>
</template>
