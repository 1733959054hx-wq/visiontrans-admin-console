<script setup>
import { computed, onMounted, reactive } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusPill from '@/components/StatusPill.vue'
import { useConfirm } from '@/composables/useConfirm'
import MerchantUploadDialog from '@/jingchen/components/MerchantUploadDialog.vue'
import { fileObjectUrl } from '@/jingchen/composables/useFileUrl'
import { useMerchantStore } from '@/jingchen/stores/merchant'
import { useUiStore } from '@/stores/ui'

/**
 * 商户素材管理（jingchen 模块业务页）:素材库真实上传 + CRUD + A/B 分流测试配置。
 * 上传走 POST /merchant/files(multipart),名称/大小/形态由落盘文件自动回填。
 */
const store = useMerchantStore()
const { confirmState, askConfirm, resolveConfirm } = useConfirm()

const materials = computed(() => store.materials || [])
const fmtCount = (n) => Number(n || 0).toLocaleString('zh-CN')
const sizeText = (kb) => (kb || 0) >= 1024 ? `${(kb / 1024).toFixed(1)} MB` : `${kb} KB`

const summary = computed(() => {
  const list = materials.value
  return [
    { label: '素材总数', value: String(list.length), icon: 'fa-photo-film' },
    { label: '使用中', value: String(list.filter((m) => m.status === '使用中').length), icon: 'fa-circle-check' },
    { label: '测试中(A/B)', value: String(list.filter((m) => m.status === '测试中').length), icon: 'fa-flask' },
    { label: 'B 组流量占比', value: store.ab ? `${store.ab.ratioB}%` : '—', icon: 'fa-scale-balanced' },
  ]
})

const typeTone = { 图片: 'green', 视频: 'blue', H5: 'amber' }
const statusTone = { 使用中: 'green', 测试中: 'amber', 已停用: 'slate' }

const MATERIAL_FIELDS = [
  { key: 'name', label: '素材名称', type: 'text', required: true, placeholder: '选文件后自动回填,可改' },
  { key: 'materialType', label: '素材形态', type: 'select', options: ['视频', '图片', 'H5'], disabled: true },
  { key: 'status', label: '投放状态', type: 'select', options: ['使用中', '测试中', '已停用'] },
]

const dialog = reactive({ open: false, title: '', record: {}, mode: 'create' })
const upload = reactive({ uploading: false, progress: 0, meta: null, error: '' })

const extOf = (name) => String(name || '').split('.').pop().toLowerCase()
const kindOfExt = (ext) => {
  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext)) return 'image'
  if (['mp4', 'mov', 'm4v', 'webm', 'avi', 'mkv'].includes(ext)) return 'video'
  return ''
}
const typeOfExt = (ext) => (kindOfExt(ext) === 'image' ? '图片' : kindOfExt(ext) === 'video' ? '视频' : '')

const openCreate = () => {
  dialog.open = true
  dialog.mode = 'create'
  dialog.title = '上传新素材'
  upload.uploading = false
  upload.progress = 0
  upload.meta = null
  upload.error = ''
  dialog.record = { name: '', materialType: '图片', sizeKb: '', status: '测试中' }
}
const openEdit = (row) => {
  dialog.open = true
  dialog.mode = 'edit'
  dialog.title = `编辑素材 · ${row.name}`
  upload.uploading = false
  upload.progress = 0
  upload.error = ''
  upload.meta = row.fileUrl
    ? { name: row.fileName || row.name, url: row.fileUrl, ext: extOf(row.fileUrl), sizeText: sizeText(row.sizeKb) }
    : null
  dialog.record = { ...row }
}
const clearFile = () => {
  upload.meta = null
  dialog.record = { ...dialog.record, fileUrl: '', fileName: '' }
}

/** 选文件 → 按扩展名定业务类型 → 先传服务器 → 回填名称/大小/形态 */
const onPick = async (file) => {
  const ext = extOf(file.name)
  const kind = kindOfExt(ext)
  if (!kind) {
    upload.error = '素材文件支持图片 / 视频格式（H5 素材无需上传文件）'
    return
  }
  upload.uploading = true
  upload.progress = 0
  upload.error = ''
  try {
    const vo = await store.upload(file, kind, (e) => {
      upload.progress = e.total ? Math.round((e.loaded / e.total) * 100) : 0
    })
    upload.meta = { name: vo.name, url: vo.url, ext: vo.ext, sizeText: sizeText(vo.sizeKb) }
    dialog.record = {
      ...dialog.record,
      name: dialog.mode === 'create' || !dialog.record.name ? vo.name.replace(/\.[^.]+$/, '') : dialog.record.name,
      materialType: typeOfExt(vo.ext),
      sizeKb: vo.sizeKb,
      fileName: vo.name,
      fileUrl: vo.url,
    }
  } catch {
    /* 提示已在 store 统一处理 */
  } finally {
    upload.uploading = false
  }
}

const submitDialog = async (form) => {
  const mode = dialog.mode
  dialog.open = false
  const payload = {
    name: form.name,
    materialType: form.materialType,
    sizeKb: Number(String(form.sizeKb ?? '').replace(/[^\d]/g, '')) || undefined,
    status: form.status,
  }
  // 新建必须已传文件(图片/视频);编辑仅在更换文件时携带
  if (upload.meta) {
    payload.fileName = upload.meta.name
    payload.fileUrl = upload.meta.url
  }
  try {
    if (mode === 'create') await store.createMaterial(payload)
    else await store.updateMaterial(dialog.record.id, payload)
  } catch {
    /* 提示已在 store 统一处理 */
  }
}

/* 文件预览（鉴权 blob） */
const preview = reactive({ open: false, name: '', url: '', ext: '', src: '' })
const openPreview = async (row) => {
  preview.open = true
  preview.name = row.fileName || row.name
  preview.url = row.fileUrl
  preview.ext = extOf(row.fileUrl)
  preview.src = ''
  preview.src = await fileObjectUrl(row.fileUrl)
}
const downloadPreview = async () => {
  const { downloadFile } = await import('@/jingchen/composables/useFileUrl')
  await downloadFile(preview.url, preview.name)
}

const removeMaterial = async (row) => {
  if (await askConfirm(`确定删除素材「${row.name}」？`, '删除素材')) {
    await store.deleteMaterial(row.id).catch(() => {})
  }
}

/* A/B 测试 */
const ab = reactive({ materialA: '', materialB: '', ratioB: 50 })
const syncAb = () => {
  const cfg = store.ab
  ab.materialA = cfg?.materialA || materials.value[0]?.name || ''
  ab.materialB = cfg?.materialB || materials.value[1]?.name || ''
  ab.ratioB = cfg?.ratioB ?? 50
}
const saveAbConfig = async () => {
  if (!ab.materialA || !ab.materialB) return
  if (ab.materialA === ab.materialB) {
    useUiStore().error('A/B 对照组不能选择同一素材')
    return
  }
  await store.saveAbConfig({ materialA: ab.materialA, materialB: ab.materialB, ratioB: ab.ratioB }).catch(() => {})
}

onMounted(() => {
  if (!store.materials) store.loadMaterials()
  if (!store.ab) store.loadAb().then(() => syncAb())
})
</script>

<template>
  <div class="p-5">
    <PageHeader title="素材管理" desc="广告素材上传 · A/B 分流测试配置">
      <template #actions>
        <button class="btn btn-primary btn-sm" :disabled="store.acting" @click="openCreate">
          <i class="fa-solid fa-upload"></i>上传素材
        </button>
      </template>
    </PageHeader>

    <!-- 摘要 -->
    <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
      <div v-for="item in summary" :key="item.label" class="card anim p-4">
        <div class="flex items-center gap-3">
          <span class="grid h-9 w-9 place-items-center rounded-xl bg-[#F1F5F9] text-slate-500">
            <i class="fa-solid" :class="item.icon"></i>
          </span>
          <div>
            <div class="text-[11px] text-sub">{{ item.label }}</div>
            <div class="num text-[19px] font-bold text-navy">{{ item.value }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-4 grid grid-cols-1 gap-4 xl:grid-cols-3">
      <!-- A/B 测试卡 -->
      <div class="card anim">
        <div class="card-h">
          <div>
            <div class="card-t">A/B 分流测试</div>
            <div class="card-s">两组素材对比 · 按流量占比分流验证点击表现</div>
          </div>
        </div>
        <div class="card-b space-y-3">
          <div>
            <div class="mb-1 text-[12px] text-sub">对照 A</div>
            <select v-model="ab.materialA" class="field">
              <option v-for="m in materials" :key="m.id" :value="m.name">{{ m.name }}</option>
            </select>
          </div>
          <div>
            <div class="mb-1 text-[12px] text-sub">对照 B</div>
            <select v-model="ab.materialB" class="field">
              <option v-for="m in materials" :key="m.id" :value="m.name">{{ m.name }}</option>
            </select>
          </div>
          <div>
            <div class="mb-1 flex items-center justify-between text-[12px] text-sub">
              <span>B 组流量占比</span>
              <span class="num font-bold text-electric">{{ ab.ratioB }}%</span>
            </div>
            <div class="flex items-center gap-3">
              <input v-model.number="ab.ratioB" type="range" class="rng" min="10" max="90" step="5" />
            </div>
          </div>
          <button class="btn btn-primary btn-sm w-full" :disabled="store.acting" @click="saveAbConfig">
            <i class="fa-solid fa-flask"></i>保存分流配置
          </button>
        </div>
      </div>

      <!-- 素材列表 -->
      <div class="card anim xl:col-span-2">
        <div class="card-h">
          <div>
            <div class="card-t">素材库</div>
            <div class="card-s">共 {{ materials.length }} 个素材 · 文件真实上传,表现数据实时统计</div>
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="tb">
            <thead>
              <tr>
                <th>素材文件</th>
                <th>形态</th>
                <th class="text-right">大小</th>
                <th class="text-right">曝光量</th>
                <th class="text-right">CTR</th>
                <th>状态</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="store.materialsLoading">
                <td colspan="7" class="py-8 text-center text-[12px] text-sub">
                  <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载素材…
                </td>
              </tr>
              <tr v-for="m in materials" v-else :key="m.id">
                <td class="text-[12.5px] font-medium">
                  <i class="fa-regular mr-1.5" :class="m.materialType === '图片' ? 'fa-image' : m.materialType === 'H5' ? 'fa-file-code' : 'fa-circle-play'"></i>{{ m.name }}
                  <span v-if="!m.fileUrl" class="ml-1 rounded bg-slate-100 px-1 py-0.5 text-[9.5px] text-slate-400">演示档案</span>
                </td>
                <td><StatusPill :text="m.materialType" :tone="typeTone[m.materialType] || 'slate'" small /></td>
                <td class="num text-right text-[12px]">{{ sizeText(m.sizeKb) }}</td>
                <td class="num text-right text-[12px]">{{ fmtCount(m.exposure) }}</td>
                <td class="num text-right text-[12px]">{{ m.ctr == null ? '—' : `${m.ctr}%` }}</td>
                <td><StatusPill :text="m.status" :tone="statusTone[m.status] || 'slate'" /></td>
                <td>
                  <div class="flex items-center justify-end gap-1">
                    <button v-if="m.fileUrl" class="btn btn-ghost btn-sm !px-2 !py-1" title="预览" @click="openPreview(m)">
                      <i class="fa-solid fa-eye"></i>
                    </button>
                    <button class="btn btn-ghost btn-sm !px-2 !py-1" title="编辑" @click="openEdit(m)">
                      <i class="fa-solid fa-pen"></i>
                    </button>
                    <button class="btn btn-ghost btn-sm !px-2 !py-1" title="删除" @click="removeMaterial(m)">
                      <i class="fa-solid fa-trash text-rose-500"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 上传 / 编辑弹窗 -->
    <MerchantUploadDialog
      :open="dialog.open"
      :title="dialog.title"
      subtitle="文件先上传服务器（multipart 真实传输）,名称 / 大小 / 形态自动识别回填"
      :fields="MATERIAL_FIELDS"
      :model-value="dialog.record"
      :loading="store.acting"
      :uploading="upload.uploading"
      :progress="upload.progress"
      :file-meta="upload.meta"
      :file-required="dialog.mode === 'create' && dialog.record.materialType !== 'H5'"
      :file-error="upload.error"
      file-hint="图片 jpg/png/gif/webp ≤20MB · 视频 mp4/mov/webm ≤500MB"
      accept=".jpg,.jpeg,.png,.gif,.webp,.bmp,.mp4,.mov,.m4v,.webm,.avi,.mkv"
      submit-text="保存素材"
      @close="dialog.open = false"
      @pick="onPick"
      @clear="clearFile"
      @submit="submitDialog"
    />

    <!-- 文件预览 -->
    <div
      v-if="preview.open"
      class="fixed inset-0 z-[70] flex items-start justify-center overflow-y-auto bg-ink/30 p-6 backdrop-blur-sm"
      @click.self="preview.open = false"
    >
      <div class="w-full max-w-2xl rounded-2xl bg-white shadow-lift">
        <div class="flex items-center justify-between border-b border-[#E5EAF0] px-5 py-3.5">
          <div class="truncate text-[14.5px] font-semibold text-ink">{{ preview.name }}</div>
          <div class="flex items-center gap-2">
            <button class="btn btn-ghost btn-sm" @click="downloadPreview">
              <i class="fa-solid fa-download"></i>下载
            </button>
            <button class="text-sub transition hover:text-ink" @click="preview.open = false">
              <i class="fa-solid fa-xmark"></i>
            </button>
          </div>
        </div>
        <div class="grid min-h-64 place-items-center px-5 py-4">
          <img v-if="preview.src" :src="preview.src" class="max-h-[480px] w-auto rounded-lg" alt="素材预览" />
          <span v-else class="text-[12px] text-sub"><i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载预览…</span>
        </div>
      </div>
    </div>

    <ConfirmDialog v-bind="confirmState" @resolve="resolveConfirm" />
  </div>
</template>
