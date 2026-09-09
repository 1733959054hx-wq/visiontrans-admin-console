<script setup>
import { ref, watch } from 'vue'

/**
 * 通用增删改查弹窗：按 fields 配置动态渲染表单。
 *
 * 设计原则：单位 / 符号"定死"在输入框里，管理员只填数字，不做事后报错。
 *
 * field: {
 *   key, label,
 *   type: 'text' | 'number' | 'select' | 'switch' | 'time' | 'datetime-local',
 *   kind?: 'percent' | 'money' | 'decimal' | 'int' | 'phone' | 'ip', // 输入净化与存储规范
 *   prefix?: '¥',                  // 固定前缀符号（只展示，不参与输入）
 *   suffix?: '%' | 'MB',           // 固定后缀单位（只展示，不参与输入）
 *   decimals?: 1,                  // 允许的小数位，超出部分输入时直接吃掉
 *   min?, max?,                    // 取值范围，失焦时静默收敛到边界
 *   options?, required?, placeholder?, disabled?
 * }
 *
 * 编辑回显时自动剥离单位（"85%" → 85），提交时自动补回（85 → "85%"），
 * 因此管理员永远不会手动输入 % / ¥ / MB 这类符号。
 */
const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  fields: { type: Array, default: () => [] },
  /** 初始表单对象（新增时传空壳，编辑时传整行数据） */
  modelValue: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'submit'])

const form = ref({})
const errors = ref({})

/** 存储值 → 编辑框里的纯数字（剥离单位符号） */
const toEditor = (value, field) => {
  const s = value === null || value === undefined ? '' : String(value).trim()
  if (s === '' || s === '—') return field.type === 'datetime-local' ? '' : s
  if (field.type === 'datetime-local') {
    // 兼容历史短格式 "09-10 18:00"：补上当前年份后再交给日期控件
    if (/^\d{2}-\d{2} \d{2}:\d{2}$/.test(s)) {
      return `${new Date().getFullYear()}-${s.replace(' ', 'T')}`
    }
    return s.replace(' ', 'T')
  }
  switch (field.kind) {
    case 'percent':
    case 'money':
    case 'decimal':
      return String(parseFloat(s.replace(/[^\d.-]/g, '')) || 0)
    case 'int':
      return s.replace(/[^\d]/g, '')
    default:
      return s
  }
}

/** 编辑框里的纯数字 → 存储值（补回单位符号） */
const toStore = (value, field) => {
  const s = value === null || value === undefined ? '' : String(value).trim()
  if (field.type === 'datetime-local') return s ? s.replace('T', ' ') : '—'
  if (!s || s === '—') return s
  const n = Number(s)
  switch (field.kind) {
    case 'percent':
      return `${Number.isNaN(n) ? 0 : n}%`
    case 'money':
      return `¥ ${Number.isNaN(n) ? 0 : n}`
    case 'decimal':
      return `${Number.isNaN(n) ? 0 : n}${field.suffix ? ` ${field.suffix}` : ''}`
    case 'int':
      return Number(s.replace(/,/g, '') || 0).toLocaleString('en-US')
    case 'phone':
      return s.includes('*') ? s : s.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
    default:
      return s
  }
}

watch(
  () => [props.open, props.modelValue],
  () => {
    const next = { ...props.modelValue }
    props.fields.forEach((f) => {
      next[f.key] = toEditor(next[f.key], f)
    })
    form.value = next
    errors.value = {}
  },
  { immediate: true, deep: true },
)

/** 输入时静默净化：非法字符与多余小数位直接吃掉，不弹错误 */
const onInput = (field) => {
  clearError(field.key)
  let s = String(form.value[field.key] ?? '')
  switch (field.kind) {
    case 'percent':
    case 'money':
    case 'decimal': {
      const d = field.decimals ?? 1
      const [intPart, ...rest] = s.replace(/[^\d.]/g, '').split('.')
      s = rest.length ? `${intPart}.${rest.join('').slice(0, d)}` : intPart
      break
    }
    case 'int':
      s = s.replace(/[^\d]/g, '')
      break
    case 'phone':
      s = s.replace(/[^\d*]/g, '').slice(0, 11)
      break
    case 'ip':
      // 每满 3 位自动补一个点，如 61135169105 → 61.135.169.105
      s = s.replace(/[^\d]/g, '').slice(0, 12).replace(/(\d{3})(?=\d)/g, '$1.')
      break
    default:
      return
  }
  if (s !== form.value[field.key]) form.value[field.key] = s
}

/** 失焦时静默收敛：超出上下限自动落到边界值 */
const onBlur = (field) => {
  const raw = form.value[field.key]
  if (raw === undefined || raw === null || raw === '' || raw === '—') return
  let n = Number(String(raw).replace(/[^\d.-]/g, ''))
  if (Number.isNaN(n)) {
    if (field.type === 'number' || field.kind) form.value[field.key] = field.min ?? 0
    return
  }
  if (field.min != null && n < field.min) n = field.min
  if (field.max != null && n > field.max) n = field.max
  if (field.type === 'number') {
    form.value[field.key] = n
  } else if (['percent', 'money', 'decimal', 'int'].includes(field.kind)) {
    form.value[field.key] = String(Number(n.toFixed(field.decimals ?? 0)))
  }
}

const inputType = (field) => {
  if (field.type === 'time' || field.type === 'datetime-local' || field.type === 'number') return field.type
  return 'text'
}

const clearError = (key) => {
  if (errors.value[key]) delete errors.value[key]
}

const submit = () => {
  const errs = {}
  const payload = {}

  props.fields.forEach((f) => {
    const raw = form.value[f.key]
    const str = raw === null || raw === undefined ? '' : String(raw).trim()

    // select / switch / 只读字段原样带出
    if (f.type === 'switch' || f.type === 'select' || f.disabled) {
      payload[f.key] = raw
      return
    }
    if (str === '') {
      // 必填拦截：无法自动补全，只能提示
      if (f.required) errs[f.key] = '该项为必填项'
      // 数字框清空时丢键，避免后端反序列化失败
      else if (f.type === 'number') return
      // 其余留空时保留原值，避免误清空系统字段或旧格式数据
      else payload[f.key] = props.modelValue[f.key] ?? (f.type === 'datetime-local' ? '—' : '')
      return
    }
    payload[f.key] = toStore(str, f)
  })

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

      <div class="grid grid-cols-2 gap-x-4 gap-y-3.5 px-5 py-4">
        <div v-for="field in fields" :key="field.key" :class="field.type === 'switch' ? 'col-span-2' : ''">
          <template v-if="field.type === 'switch'">
            <div class="flex items-center justify-between rounded-lg border border-line px-3 py-2">
              <span class="text-[12.5px] font-medium text-slate-700">{{ field.label }}</span>
              <span
                class="sw"
                :class="{ on: !!form[field.key] }"
                @click="form[field.key] = !form[field.key]"
              ></span>
            </div>
          </template>
          <template v-else>
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
              <span
                v-if="field.prefix"
                class="pointer-events-none absolute left-2.5 top-1/2 -translate-y-1/2 select-none text-[12.5px] text-slate-400"
              >{{ field.prefix }}</span>
              <input
                v-model="form[field.key]"
                :type="inputType(field)"
                :step="field.type === 'time' ? 1 : undefined"
                class="field"
                :class="[
                  field.prefix ? '!pl-7' : '',
                  field.suffix ? '!pr-9' : '',
                  errors[field.key] ? '!border-rose-400' : '',
                ]"
                :placeholder="field.placeholder || ''"
                :disabled="field.disabled"
                :maxlength="field.maxlength"
                @input="onInput(field)"
                @blur="onBlur(field)"
              />
              <span
                v-if="field.suffix"
                class="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 select-none text-[12.5px] text-slate-400"
              >{{ field.suffix }}</span>
            </div>
            <p v-if="errors[field.key]" class="mt-1 text-[11px] text-rose-500">
              <i class="fa-solid fa-circle-exclamation mr-1"></i>{{ errors[field.key] }}
            </p>
          </template>
        </div>
      </div>

      <div class="flex items-center justify-between border-t border-[#E5EAF0] px-5 py-3.5">
        <span v-if="Object.keys(errors).length" class="text-[11.5px] text-rose-500">
          <i class="fa-solid fa-circle-exclamation mr-1"></i>还有 {{ Object.keys(errors).length }} 项必填未填
        </span>
        <span v-else class="text-[11px] text-slate-400">带 * 为必填项 · 单位已固定，只需填数字</span>
        <div class="flex gap-2">
          <button class="btn btn-ghost btn-sm" @click="emit('close')">取消</button>
          <button class="btn btn-primary btn-sm" :disabled="loading" @click="submit">
            <i v-if="loading" class="fa-solid fa-circle-notch fa-spin"></i>保存
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
