<script setup>
import { computed, onMounted, ref, watch } from 'vue'

import { fileObjectUrl } from '@/jingchen/composables/useFileUrl'

/**
 * 商户文件选择区（jingchen 模块）：点击 / 拖拽选文件 → 交给父级上传。
 *
 * props.fileMeta  形如 { name, sizeText, url, ext }，有值即视为"已选好文件"展示回执
 * props.uploading 上传中遮罩（父级控制）
 * emits pick(File) / clear()
 */
const props = defineProps({
  accept: { type: String, default: '' },
  hint: { type: String, default: '' },
  uploading: { type: Boolean, default: false },
  /** 上传进度 0~100 */
  progress: { type: Number, default: 0 },
  fileMeta: { type: Object, default: null },
  /** 预览时无 fileMeta 但想直接展示某文件（如字幕文件回显） */
  url: { type: String, default: '' },
  ext: { type: String, default: '' },
  name: { type: String, default: '' },
})

const emit = defineEmits(['pick', 'clear'])
const input = ref(null)
const dragging = ref(false)
const previewUrl = ref('')

const meta = computed(() => {
  if (props.fileMeta) return props.fileMeta
  if (props.url) return { name: props.name, sizeText: '', url: props.url, ext: props.ext }
  return null
})

const isImage = computed(() => ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(String(meta.value?.ext || '').toLowerCase()))
const isVideo = computed(() => ['mp4', 'webm', 'mov', 'm4v'].includes(String(meta.value?.ext || '').toLowerCase()))

/** 图片 / 视频预览需要鉴权取回 blob */
const refreshPreview = async () => {
  const url = meta.value?.url
  previewUrl.value = ''
  if (url && (isImage.value || isVideo.value)) {
    previewUrl.value = await fileObjectUrl(url)
  }
}
watch(() => [meta.value?.url, isImage.value, isVideo.value], refreshPreview)
onMounted(refreshPreview)

const choose = () => {
  if (props.uploading) return
  input.value?.click()
}
const onInput = (e) => {
  const file = e.target.files?.[0]
  if (file) emit('pick', file)
  e.target.value = ''
}
const onDrop = (e) => {
  dragging.value = false
  if (props.uploading) return
  const file = e.dataTransfer?.files?.[0]
  if (file) emit('pick', file)
}
</script>

<template>
  <div>
    <div
      v-if="!meta"
      class="rounded-lg border-2 border-dashed border-blue-200 bg-blue-50/50 px-4 py-5 text-center transition hover:bg-blue-50/80"
      :class="{ '!border-electric bg-blue-50': dragging, 'pointer-events-none opacity-60': uploading }"
      @click="choose"
      @dragover.prevent="dragging = true"
      @dragleave="dragging = false"
      @drop.prevent="onDrop"
    >
      <i class="fa-solid fa-cloud-arrow-up text-[22px] text-electric"></i>
      <div class="mt-1.5 text-[12.5px] font-semibold text-ink">点击选择或拖入文件</div>
      <div class="mt-0.5 text-[10.5px] text-sub">{{ hint }}</div>
    </div>

    <div v-else class="flex items-center gap-3 rounded-lg border border-line bg-[#F8FAFC] px-3 py-2.5">
      <div class="grid h-12 w-16 flex-none place-items-center overflow-hidden rounded-md border border-line bg-white">
        <img v-if="isImage && previewUrl" :src="previewUrl" class="h-full w-full object-cover" alt="预览" />
        <video v-else-if="isVideo && previewUrl" :src="previewUrl" class="h-full w-full object-cover" muted></video>
        <i v-else class="fa-solid fa-file text-[18px] text-slate-400"></i>
      </div>
      <div class="min-w-0 flex-1">
        <div class="truncate text-[12.5px] font-semibold text-ink" :title="meta.name">{{ meta.name }}</div>
        <div v-if="meta.sizeText" class="mt-0.5 num text-[11px] text-sub">{{ meta.sizeText }}</div>
        <div v-if="uploading" class="mt-1.5">
          <div class="h-1 w-full overflow-hidden rounded bg-slate-200">
            <div class="h-full rounded bg-electric transition-all" :style="`width:${progress}%`"></div>
          </div>
          <div class="num mt-0.5 text-[10.5px] text-sub">上传中 {{ progress }}%</div>
        </div>
      </div>
      <button
        v-if="!uploading"
        class="btn btn-ghost btn-sm !px-2 !py-1"
        title="重新选择"
        @click="choose"
      >
        <i class="fa-solid fa-rotate"></i>
      </button>
      <button
        v-if="!uploading"
        class="btn btn-ghost btn-sm !px-2 !py-1"
        title="移除"
        @click="emit('clear')"
      >
        <i class="fa-solid fa-trash text-rose-500"></i>
      </button>
    </div>

    <input ref="input" type="file" class="hidden" :accept="accept" @change="onInput" />
  </div>
</template>
