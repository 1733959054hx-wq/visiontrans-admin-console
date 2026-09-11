<script setup>
import { ref, watch } from 'vue'

import MerchantFileDrop from '@/jingchen/components/MerchantFileDrop.vue'

/**
 * 商户上传弹窗（jingchen 模块）：文件区 + 字段表单。
 *
 * 与通用 CrudDialog 同风格,但首屏是真实文件选择区:
 * 选文件 → 父级上传 → 回填元数据（名称/大小自动带出）→ 提交建档。
 * 编辑模式可换文件（fileMeta 传入旧文件即可回显）。
 */
const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  /** text / number / select / disabled 字段（复用 CrudDialog 的 schema 约定） */
  fields: { type: Array, default: () => [] },
  modelValue: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false },
  uploading: { type: Boolean, default: false },
  progress: { type: Number, default: 0 },
  /** 已上传文件回执 { name, sizeText, url, ext } */
  fileMeta: { type: Object, default: null },
  fileRequired: { type: Boolean, default: false },
  fileError: { type: String, default: '' },
  accept: { type: String, default: '' },
  fileHint: { type: String, default: '' },
  /** 文件区提示语（如「图片 ≤20MB · 视频 ≤500MB」） */
  submitText: { type: String, default: '保存' },
})

const emit = defineEmits(['close', 'submit', 'pick', 'clear'])

const form = ref({})
const errors = ref({})

watch(
  () => [props.open, props.modelValue],
  () => {
    form.value = { ...props.modelValue }
    errors.value = {}
  },
  { immediate: true },
)

const inputType = (field) => (field.type === 'number' ? 'number' : 'text')

const submit = () => {
  const errs = {}
  const payload = {}
  props.fields.forEach((f) => {
    const raw = form.value[f.key]
    const str = raw === null || raw === undefined ? '' : String(raw).trim()
    if (f.type === 'select' || f.disabled) {
      payload[f.key] = raw
      return
    }
    if (str === '') {
      if (f.required) errs[f.key] = '该项为必填项'
      else if (f.type === 'number') return
      else payload[f.key] = props.modelValue[f.key] ?? ''
      return
    }
    payload[f.key] = f.type === 'number' ? Number(str) : str
  })
  if (props.fileRequired && !props.fileMeta && !props.uploading) {
    errors.value.__file = '请先选择并上传文件'
    return
  }
  errors.value = errs
  if (Object.keys(errs).length > 0) return
  emit('submit', payload)
}
</script>

<template>
  <div
    v-if="open"
    class="fixed inset-0 z-[70] flex items-start justify-center overflow-y-auto bg-ink/30 p-6 backdrop-blur-sm"
    @click.self="emit('close')"
  >
    <div class="w-full max-w-lg rounded-2xl bg-white shadow-lift">
      <div class="flex items-center justify-between border-b border-[#E5EAF0] px-5 py-3.5">
        <div class="text-[14.5px] font-semibold text-ink">{{ title }}</div>
        <button class="text-sub transition hover:text-ink" @click="emit('close')">
          <i class="fa-solid fa-xmark"></i>
        </button>
      </div>

      <div class="space-y-3.5 px-5 py-4">
        <div v-if="subtitle" class="text-[11.5px] text-sub">{{ subtitle }}</div>
        <div>
          <label class="lbl">文件<span class="text-rose-500">*</span></label>
          <MerchantFileDrop
            :accept="accept"
            :hint="fileHint"
            :uploading="uploading"
            :progress="progress"
            :file-meta="fileMeta"
            @pick="(file) => emit('pick', file)"
            @clear="emit('clear')"
          />
          <p v-if="errors.__file || fileError" class="mt-1 text-[11px] text-rose-500">
            <i class="fa-solid fa-circle-exclamation mr-1"></i>{{ errors.__file || fileError }}
          </p>
        </div>

        <div class="grid grid-cols-2 gap-x-4 gap-y-3.5">
          <div v-for="field in fields" :key="field.key">
            <label class="lbl">
              {{ field.label }}
              <span v-if="field.required" class="text-rose-500">*</span>
            </label>
            <select
              v-if="field.type === 'select'"
              v-model="form[field.key]"
              class="field"
              :disabled="field.disabled"
            >
              <option v-for="option in field.options || []" :key="option" :value="option">{{ option }}</option>
            </select>
            <div v-else class="relative">
              <input
                v-model="form[field.key]"
                :type="inputType(field)"
                class="field"
                :class="[field.suffix ? '!pr-9' : '', errors[field.key] ? '!border-rose-400' : '']"
                :placeholder="field.placeholder || ''"
                :disabled="field.disabled"
                :maxlength="field.maxlength"
              />
              <span
                v-if="field.suffix"
                class="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 select-none text-[12.5px] text-slate-400"
              >{{ field.suffix }}</span>
            </div>
            <p v-if="errors[field.key]" class="mt-1 text-[11px] text-rose-500">
              <i class="fa-solid fa-circle-exclamation mr-1"></i>{{ errors[field.key] }}
            </p>
          </div>
        </div>
      </div>

      <div class="flex items-center justify-between border-t border-[#E5EAF0] px-5 py-3.5">
        <span class="text-[11px] text-slate-400">文件先传服务器,名称与大小自动回填</span>
        <div class="flex gap-2">
          <button class="btn btn-ghost btn-sm" :disabled="uploading" @click="emit('close')">取消</button>
          <button class="btn btn-primary btn-sm" :disabled="loading || uploading" @click="submit">
            <i v-if="loading || uploading" class="fa-solid fa-circle-notch fa-spin"></i>{{ submitText }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
