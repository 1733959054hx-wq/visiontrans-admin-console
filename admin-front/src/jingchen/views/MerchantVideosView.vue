<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusPill from '@/components/StatusPill.vue'
import { useConfirm } from '@/composables/useConfirm'
import MerchantFileDrop from '@/jingchen/components/MerchantFileDrop.vue'
import MerchantUploadDialog from '@/jingchen/components/MerchantUploadDialog.vue'
import { useMerchantStore } from '@/jingchen/stores/merchant'

/**
 * 商户视频接入（jingchen 模块业务页）:真实上传建档 / 元数据维护 / 字幕管理(独立表) / 播放数据。
 * 上传走 POST /merchant/files(multipart),大小按落盘文件折算;字幕在"字幕管理"弹窗按语种维护。
 */
const store = useMerchantStore()
const router = useRouter()
const { confirmState, askConfirm, resolveConfirm } = useConfirm()

const videos = computed(() => store.videos || [])
const fmtCount = (n) => Number(n || 0).toLocaleString('zh-CN')
const fmtDur = (sec) => {
  const s = Number(sec || 0)
  const m = String(Math.floor(s / 60)).padStart(2, '0')
  const ss = String(Math.floor(s % 60)).padStart(2, '0')
  return `00:${m}:${ss}`
}

const summary = computed(() => {
  const list = videos.value
  return [
    { label: '视频档案', value: `${list.length} 部`, icon: 'fa-film' },
    { label: '累计播放', value: fmtCount(list.reduce((s, v) => s + (v.plays || 0), 0)), icon: 'fa-circle-play' },
    { label: '已上架', value: `${list.filter((v) => v.status === '已上架').length} 部`, icon: 'fa-circle-check' },
    { label: '处理中', value: `${list.filter((v) => v.status === '转码中').length} 部`, icon: 'fa-gears' },
  ]
})

const statusTone = { 已上架: 'green', 已就绪: 'blue', 转码中: 'amber' }

const VIDEO_FIELDS = [
  { key: 'name', label: '视频名称', type: 'text', required: true, placeholder: '选文件后自动回填,可改' },
  { key: 'lang', label: '语种', type: 'select', options: ['多语', '中', '英', '日', '韩', '法', '德', '阿'] },
  { key: 'region', label: '授权区域', type: 'select', options: ['全球', '亚太', '欧洲'] },
  { key: 'status', label: '处理状态', type: 'select', options: ['转码中', '已就绪', '已上架'] },
]

const dialog = reactive({ open: false, title: '', record: {}, mode: 'create' })
const upload = reactive({ uploading: false, progress: 0, meta: null, error: '' })

const openUpload = () => {
  dialog.open = true
  dialog.mode = 'create'
  dialog.title = '上传视频'
  upload.uploading = false
  upload.progress = 0
  upload.meta = null
  upload.error = ''
  dialog.record = { name: '', lang: '多语', region: '全球', status: '转码中' }
}
const openEdit = (row) => {
  dialog.open = true
  dialog.mode = 'edit'
  dialog.title = `元数据编辑 · ${row.name}`
  upload.uploading = false
  upload.progress = 0
  upload.error = ''
  upload.meta = row.fileUrl
    ? { name: row.fileName || row.name, url: row.fileUrl, ext: extOf(row.fileUrl), sizeText: `${row.sizeGb} GB` }
    : null
  dialog.record = { ...row }
}
const clearFile = () => {
  upload.meta = null
  dialog.record = { ...dialog.record, fileUrl: '', fileName: '' }
}

const extOf = (name) => String(name || '').split('.').pop().toLowerCase()
const VIDEO_EXTS = ['mp4', 'mov', 'm4v', 'webm', 'avi', 'mkv']

const onPick = async (file) => {
  const ext = extOf(file.name)
  if (!VIDEO_EXTS.includes(ext)) {
    upload.error = '视频支持 mp4 / mov / webm / mkv 等格式'
    return
  }
  upload.uploading = true
  upload.progress = 0
  upload.error = ''
  try {
    const vo = await store.upload(file, 'video', (e) => {
      upload.progress = e.total ? Math.round((e.loaded / e.total) * 100) : 0
    })
    upload.meta = { name: vo.name, url: vo.url, ext: vo.ext, sizeText: `${(vo.sizeBytes / 1e9).toFixed(2)} GB` }
    dialog.record = {
      ...dialog.record,
      name: dialog.mode === 'create' || !dialog.record.name ? vo.name.replace(/\.[^.]+$/, '') : dialog.record.name,
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
    lang: form.lang,
    status: form.status,
    region: form.region,
  }
  if (upload.meta) {
    payload.fileName = upload.meta.name
    payload.fileUrl = upload.meta.url
  }
  try {
    if (mode === 'create') await store.createVideo(payload)
    else await store.updateVideo(dialog.record.id, payload)
  } catch {
    /* 提示已在 store 统一处理 */
  }
}
const removeVideo = async (row) => {
  if (await askConfirm(`确定删除视频档案「${row.name}」？删除后其字幕记录一并失效。`, '删除视频')) {
    await store.deleteVideo(row.id).catch(() => {})
  }
}

/* ------------------------------ 字幕管理(独立表) ------------------------------ */
const SUBTITLE_LANGS = ['中', '英', '日', '韩', '法', '德', '阿', '俄', '西']
const subDialog = reactive({ open: false, video: null })
const subUpload = reactive({ uploading: false, progress: 0, meta: null, error: '' })
const subForm = reactive({ lang: '中' })
const subtitles = computed(() => store.subtitles || [])

const openSubtitles = async (row) => {
  subDialog.open = true
  subDialog.video = row
  subUpload.uploading = false
  subUpload.progress = 0
  subUpload.meta = null
  subUpload.error = ''
  subForm.lang = SUBTITLE_LANGS.find((l) => !(row.subtitleLangs || '').includes(l)) || SUBTITLE_LANGS[0]
  await store.loadSubtitles(row.id)
}

const pickSubtitleFile = async (file) => {
  const ext = extOf(file.name)
  if (!['srt', 'vtt'].includes(ext)) {
    subUpload.error = '字幕仅支持 SRT / VTT 文件'
    return
  }
  subUpload.uploading = true
  subUpload.progress = 0
  subUpload.error = ''
  try {
    const vo = await store.upload(file, 'subtitle', (e) => {
      subUpload.progress = e.total ? Math.round((e.loaded / e.total) * 100) : 0
    })
    subUpload.meta = { name: vo.name, url: vo.url, ext: vo.ext, sizeText: `${vo.sizeKb} KB` }
  } catch {
    /* 提示已在 store 统一处理 */
  } finally {
    subUpload.uploading = false
  }
}

const addSubtitle = async () => {
  const video = subDialog.video
  if (!video) return
  try {
    await store.createSubtitle(video.id, {
      lang: subForm.lang,
      fileName: subUpload.meta?.name || '',
      fileUrl: subUpload.meta?.url || '',
    })
    subUpload.meta = null
    subForm.lang = SUBTITLE_LANGS.find((l) => !(video.subtitleLangs || '').includes(l)) || SUBTITLE_LANGS[0]
  } catch {
    /* 提示已在 store 统一处理 */
  }
}

const removeSubtitle = async (sub) => {
  if (await askConfirm(`确定删除「${subDialog.video.name}」的「${sub.lang}」字幕？`, '删除字幕')) {
    await store.deleteSubtitle(subDialog.video.id, sub.id).catch(() => {})
  }
}

const downloadSubtitle = async (sub) => {
  const { downloadFile } = await import('@/jingchen/composables/useFileUrl')
  await downloadFile(sub.fileUrl, sub.fileName || `${subDialog.video.name}_${sub.lang}.srt`)
}

onMounted(() => {
  if (!store.videos) store.loadVideos()
})
</script>

<template>
  <div class="p-5">
    <PageHeader title="视频接入" desc="视频上传 · 字幕管理 · 授权区域与播放数据">
      <template #actions>
        <button class="btn btn-ghost btn-sm" @click="router.push({ name: 'merchantVideoStats' })">
          <i class="fa-solid fa-chart-simple"></i>播放统计
        </button>
        <button class="btn btn-primary btn-sm" :disabled="store.acting" @click="openUpload">
          <i class="fa-solid fa-upload"></i>上传视频
        </button>
      </template>
    </PageHeader>

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

    <div class="card anim mt-4">
      <div class="card-h">
        <div>
          <div class="card-t">视频库</div>
          <div class="card-s">共 {{ videos.length }} 部 · 文件真实上传,字幕按语种独立维护</div>
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="tb">
          <thead>
            <tr>
              <th>视频文件</th>
              <th>语种</th>
              <th class="text-right">大小</th>
              <th class="text-right">播放量</th>
              <th class="text-right">完播率</th>
              <th>字幕</th>
              <th>状态</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="store.videosLoading">
              <td colspan="8" class="py-8 text-center text-[12px] text-sub">
                <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载视频…
              </td>
            </tr>
            <tr v-for="v in videos" v-else :key="v.id">
              <td class="text-[12.5px] font-medium">
                <i class="fa-solid fa-clapperboard mr-1.5 text-brand-500"></i>{{ v.name }}
                <span v-if="!v.fileUrl" class="ml-1 rounded bg-slate-100 px-1 py-0.5 text-[9.5px] text-slate-400">演示档案</span>
              </td>
              <td><StatusPill :text="v.lang" tone="slate" small /></td>
              <td class="num text-right text-[12px]">{{ v.sizeGb }} GB</td>
              <td class="num text-right text-[12px]">{{ fmtCount(v.plays) }}</td>
              <td class="num text-right text-[12px]">{{ v.finishRate == null ? '—' : `${v.finishRate}%` }}</td>
              <td class="text-[11.5px] text-sub">{{ v.subtitleLangs || '—' }}</td>
              <td><StatusPill :text="v.status" :tone="statusTone[v.status] || 'slate'" /></td>
              <td>
                <div class="flex items-center justify-end gap-1">
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="字幕管理" @click="openSubtitles(v)">
                    <i class="fa-solid fa-closed-captioning"></i>
                  </button>
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="元数据编辑" @click="openEdit(v)">
                    <i class="fa-solid fa-pen"></i>
                  </button>
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="删除" @click="removeVideo(v)">
                    <i class="fa-solid fa-trash text-rose-500"></i>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 上传 / 编辑弹窗 -->
    <MerchantUploadDialog
      :open="dialog.open"
      :title="dialog.title"
      subtitle="文件先上传服务器,大小按落盘文件自动折算(GB);时长转码后识别"
      :fields="VIDEO_FIELDS"
      :model-value="dialog.record"
      :loading="store.acting"
      :uploading="upload.uploading"
      :progress="upload.progress"
      :file-meta="upload.meta"
      :file-required="dialog.mode === 'create'"
      :file-error="upload.error"
      file-hint="mp4 / mov / webm / mkv · ≤500MB"
      accept=".mp4,.mov,.m4v,.webm,.avi,.mkv"
      submit-text="保存视频"
      @close="dialog.open = false"
      @pick="onPick"
      @clear="clearFile"
      @submit="submitDialog"
    />

    <!-- 字幕管理弹窗 -->
    <div
      v-if="subDialog.open"
      class="fixed inset-0 z-[70] flex items-start justify-center overflow-y-auto bg-ink/30 p-6 backdrop-blur-sm"
      @click.self="subDialog.open = false"
    >
      <div class="w-full max-w-xl rounded-2xl bg-white shadow-lift">
        <div class="flex items-center justify-between border-b border-[#E5EAF0] px-5 py-3.5">
          <div class="text-[14.5px] font-semibold text-ink">
            <i class="fa-solid fa-closed-captioning mr-1.5 text-electric"></i>字幕管理 · {{ subDialog.video?.name }}
          </div>
          <button class="text-sub transition hover:text-ink" @click="subDialog.open = false">
            <i class="fa-solid fa-xmark"></i>
          </button>
        </div>

        <div class="space-y-4 px-5 py-4">
          <!-- 已有字幕 -->
          <div class="overflow-hidden rounded-lg border border-line">
            <table class="tb">
              <thead>
                <tr>
                  <th>语种</th>
                  <th>字幕文件</th>
                  <th>格式</th>
                  <th>更新时间</th>
                  <th class="text-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="store.subtitlesLoading">
                  <td colspan="5" class="py-6 text-center text-[12px] text-sub">
                    <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载字幕…
                  </td>
                </tr>
                <tr v-else-if="!subtitles.length">
                  <td colspan="5" class="py-6 text-center text-[12px] text-sub">暂无字幕,先在下方按语种添加</td>
                </tr>
                <tr v-for="sub in subtitles" v-else :key="sub.id">
                  <td><StatusPill :text="sub.lang" tone="blue" small /></td>
                  <td class="max-w-56 truncate text-[12px]" :title="sub.fileName">
                    {{ sub.fileName || '—' }}
                    <span v-if="!sub.fileUrl" class="ml-1 rounded bg-slate-100 px-1 py-0.5 text-[9.5px] text-slate-400">未传文件</span>
                  </td>
                  <td class="text-[11.5px] text-sub">{{ sub.format }}</td>
                  <td class="num text-[11.5px] text-sub">{{ sub.uploadedAt }}</td>
                  <td>
                    <div class="flex items-center justify-end gap-1">
                      <button v-if="sub.fileUrl" class="btn btn-ghost btn-sm !px-2 !py-1" title="下载" @click="downloadSubtitle(sub)">
                        <i class="fa-solid fa-download"></i>
                      </button>
                      <button class="btn btn-ghost btn-sm !px-2 !py-1" title="删除" @click="removeSubtitle(sub)">
                        <i class="fa-solid fa-trash text-rose-500"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 添加字幕 -->
          <div class="rounded-lg border border-line bg-[#F8FAFC] p-3.5">
            <div class="mb-2 text-[12.5px] font-semibold text-ink">添加字幕</div>
            <div class="grid grid-cols-1 gap-3 md:grid-cols-[120px_1fr]">
              <div>
                <label class="lbl">语种<span class="text-rose-500">*</span></label>
                <select v-model="subForm.lang" class="field">
                  <option v-for="l in SUBTITLE_LANGS" :key="l" :value="l">{{ l }}</option>
                </select>
              </div>
              <div>
                <label class="lbl">字幕文件（可选,先上传 SRT / VTT）</label>
                <MerchantFileDrop
                  accept=".srt,.vtt"
                  hint="SRT / VTT · ≤5MB"
                  :uploading="subUpload.uploading"
                  :progress="subUpload.progress"
                  :file-meta="subUpload.meta"
                  @pick="pickSubtitleFile"
                  @clear="subUpload.meta = null"
                />
                <p v-if="subUpload.error" class="mt-1 text-[11px] text-rose-500">
                  <i class="fa-solid fa-circle-exclamation mr-1"></i>{{ subUpload.error }}
                </p>
              </div>
            </div>
            <button class="btn btn-primary btn-sm mt-3 w-full" :disabled="store.acting || subUpload.uploading" @click="addSubtitle">
              <i class="fa-solid fa-plus"></i>添加「{{ subForm.lang }}」字幕
            </button>
          </div>
        </div>
      </div>
    </div>

    <ConfirmDialog v-bind="confirmState" @resolve="resolveConfirm" />
  </div>
</template>
