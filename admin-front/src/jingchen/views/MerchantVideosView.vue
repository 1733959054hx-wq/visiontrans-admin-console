<script setup>
import { computed, onMounted, reactive } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusPill from '@/components/StatusPill.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useMerchantStore } from '@/jingchen/stores/merchant'

/**
 * 商户视频接入（jingchen 模块业务页）:视频档案 / 元数据 / 字幕语言 / 播放数据。
 * "上传"为演示动作:新建档案后状态为转码中,编辑可推进到已就绪 / 已上架。
 */
const store = useMerchantStore()
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
  { key: 'name', label: '视频文件名', type: 'text', required: true, placeholder: '如：JP_Tokyo_Transit_4K_Master.mp4' },
  { key: 'lang', label: '语种', type: 'text', placeholder: '如 JA·JP' },
  { key: 'durationSec', label: '时长(秒)', type: 'number', min: 0 },
  { key: 'sizeGb', label: '大小', type: 'text', kind: 'decimal', suffix: 'GB', decimals: 2, min: 0, placeholder: '12.40' },
  { key: 'status', label: '处理状态', type: 'select', options: ['转码中', '已就绪', '已上架'] },
  { key: 'region', label: '授权区域', type: 'select', options: ['全球', '亚太', '欧洲'] },
  { key: 'subtitleLangs', label: '字幕语言(逗号分隔)', type: 'text', placeholder: '中,英,日' },
]

const dialog = reactive({ open: false, title: '', record: {}, mode: 'create' })
const openUpload = () => {
  dialog.open = true
  dialog.mode = 'create'
  dialog.title = '上传视频(演示:建立档案)'
  dialog.record = { name: '', lang: '多语', durationSec: 1800, sizeGb: 8, status: '转码中', region: '全球', subtitleLangs: '中,英' }
}
const openEdit = (row) => {
  dialog.open = true
  dialog.mode = 'edit'
  dialog.title = `元数据编辑 · ${row.name}`
  dialog.record = { ...row }
}
const submitDialog = async (form) => {
  const mode = dialog.mode
  dialog.open = false
  const payload = {
    name: form.name,
    lang: form.lang,
    durationSec: Number(form.durationSec) || 0,
    sizeGb: Number(String(form.sizeGb).replace(/[^\d.]/g, '')) || 0,
    status: form.status,
    region: form.region,
    subtitleLangs: form.subtitleLangs,
  }
  try {
    if (mode === 'create') await store.createVideo(payload)
    else await store.updateVideo(dialog.record.id, payload)
  } catch {
    /* 提示已在 store 统一处理 */
  }
}
const removeVideo = async (row) => {
  if (await askConfirm(`确定删除视频档案「${row.name}」？`, '删除视频')) {
    await store.deleteVideo(row.id).catch(() => {})
  }
}

onMounted(() => {
  if (!store.videos) store.loadVideos()
})
</script>

<template>
  <div class="p-5">
    <PageHeader title="视频接入" desc="视频档案 · 字幕语言 · 授权区域与播放数据">
      <template #actions>
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
          <div class="card-s">共 {{ videos.length }} 部 · 支持元数据编辑与字幕语言绑定</div>
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="tb">
          <thead>
            <tr>
              <th>视频文件</th>
              <th>语种</th>
              <th class="text-right">时长</th>
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
              <td colspan="9" class="py-8 text-center text-[12px] text-sub">
                <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载视频…
              </td>
            </tr>
            <tr v-for="v in videos" v-else :key="v.id">
              <td class="text-[12.5px] font-medium">
                <i class="fa-solid fa-clapperboard mr-1.5 text-brand-500"></i>{{ v.name }}
              </td>
              <td><StatusPill :text="v.lang" tone="slate" small /></td>
              <td class="num text-right text-[12px]">{{ fmtDur(v.durationSec) }}</td>
              <td class="num text-right text-[12px]">{{ v.sizeGb }} GB</td>
              <td class="num text-right text-[12px]">{{ fmtCount(v.plays) }}</td>
              <td class="num text-right text-[12px]">{{ v.finishRate == null ? '—' : `${v.finishRate}%` }}</td>
              <td class="text-[11.5px] text-sub">{{ v.subtitleLangs || '—' }}</td>
              <td><StatusPill :text="v.status" :tone="statusTone[v.status] || 'slate'" /></td>
              <td>
                <div class="flex items-center justify-end gap-1">
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

    <CrudDialog
      :open="dialog.open"
      :title="dialog.title"
      :fields="VIDEO_FIELDS"
      :model-value="dialog.record"
      :loading="store.acting"
      @close="dialog.open = false"
      @submit="submitDialog"
    />
    <ConfirmDialog v-bind="confirmState" @resolve="resolveConfirm" />
  </div>
</template>
