<script setup>
import { computed, onMounted, reactive } from 'vue'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import CrudDialog from '@/components/CrudDialog.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusPill from '@/components/StatusPill.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useMerchantStore } from '@/jingchen/stores/merchant'

/**
 * 商户商品管理（jingchen 模块业务页,内容分销-商品管理 / 定价策略）。
 * 上架 / 下架 / 编辑(含折扣定价联动) / 删除。
 */
const store = useMerchantStore()
const { confirmState, askConfirm, resolveConfirm } = useConfirm()

const goods = computed(() => store.goods || [])

const summary = computed(() => {
  const list = goods.value
  return [
    { label: '商品总数', value: `${list.length} 个`, icon: 'fa-box-open' },
    { label: '在售', value: `${list.filter((g) => g.status === '在售').length} 个`, icon: 'fa-circle-check' },
    { label: '已下架', value: `${list.filter((g) => g.status === '已下架').length} 个`, icon: 'fa-box-archive' },
    { label: '累计销量', value: `${Number(list.reduce((s, g) => s + (g.salesCount || 0), 0)).toLocaleString('zh-CN')} 份`, icon: 'fa-cart-shopping' },
  ]
})

const typeTone = { 专业词典: 'blue', 视频课程: 'blue', 文化包: 'amber' }
const statusTone = { 在售: 'green', 已下架: 'slate' }

const GOODS_FIELDS = [
  { key: 'goodsName', label: '商品名称', type: 'text', required: true, placeholder: '如：出境医疗急救术语包' },
  { key: 'goodsType', label: '商品类型', type: 'select', options: ['专业词典', '视频课程', '文化包'] },
  { key: 'description', label: '商品描述', type: 'text', placeholder: '选填', disabled: false },
  { key: 'price', label: '原价(元)', type: 'number', min: 0, required: true },
  { key: 'discount', label: '折扣(%)', type: 'number', min: 30, max: 100 },
]

const dialog = reactive({ open: false, title: '', record: {}, mode: 'create' })
const openCreate = () => {
  dialog.open = true
  dialog.mode = 'create'
  dialog.title = '商品上架'
  dialog.record = { goodsName: '', goodsType: '专业词典', description: '', price: 68, discount: 100 }
}
const openEdit = (row) => {
  dialog.open = true
  dialog.mode = 'edit'
  dialog.title = `编辑商品 · ${row.goodsName}`
  dialog.record = { ...row }
}
const submitDialog = async (form) => {
  const mode = dialog.mode
  dialog.open = false
  const payload = {
    goodsName: form.goodsName,
    goodsType: form.goodsType,
    description: form.description || '',
    price: Number(form.price) || 0,
    discount: Number(form.discount) || 100,
  }
  try {
    if (mode === 'create') await store.createGoods(payload)
    else await store.updateGoods(dialog.record.id, payload)
  } catch {
    /* 提示已在 store 统一处理 */
  }
}
const toggleShelf = async (row) => {
  try {
    if (row.status === '在售') await store.unshelfGoods(row.id)
    else await store.shelfGoods(row.id)
  } catch {
    /* 提示已在 store 统一处理 */
  }
}
const removeGoods = async (row) => {
  if (await askConfirm(`确定删除商品「${row.goodsName}」？`, '删除商品')) {
    await store.deleteGoods(row.id).catch(() => {})
  }
}

onMounted(() => {
  if (!store.goods) store.loadGoods()
})
</script>

<template>
  <div class="p-5">
    <PageHeader title="商品管理" desc="内容分销 · 商品上架 / 编辑 / 下架与折扣定价">
      <template #actions>
        <button class="btn btn-primary btn-sm" :disabled="store.acting" @click="openCreate">
          <i class="fa-solid fa-plus"></i>商品上架
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
          <div class="card-t">商品列表</div>
          <div class="card-s">共 {{ goods.length }} 个商品 · 折扣定价保存后自动计算折后价</div>
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="tb">
          <thead>
            <tr>
              <th>商品 / 描述</th>
              <th>类型</th>
              <th class="text-right">原价</th>
              <th class="text-right">折扣</th>
              <th class="text-right">售价</th>
              <th class="text-right">累计销量</th>
              <th>状态</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="store.goodsLoading">
              <td colspan="8" class="py-8 text-center text-[12px] text-sub">
                <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>加载商品…
              </td>
            </tr>
            <tr v-for="g in goods" v-else :key="g.id">
              <td>
                <div class="text-[12.5px] font-medium text-ink">{{ g.goodsName }}</div>
                <div class="text-[10.5px] text-sub">{{ g.description || '—' }}</div>
              </td>
              <td><StatusPill :text="g.goodsType" :tone="typeTone[g.goodsType] || 'slate'" small /></td>
              <td class="num text-right text-[12px]">¥ {{ Number(g.price || 0).toLocaleString('zh-CN') }}</td>
              <td class="num text-right text-[12px]">{{ g.discount }}%</td>
              <td class="num text-right text-[13px] font-bold text-navy">¥ {{ Number(g.salePrice || 0).toLocaleString('zh-CN') }}</td>
              <td class="num text-right text-[12px]">{{ Number(g.salesCount || 0).toLocaleString('zh-CN') }}</td>
              <td><StatusPill :text="g.status" :tone="statusTone[g.status] || 'slate'" /></td>
              <td>
                <div class="flex items-center justify-end gap-1">
                  <button
                    class="btn btn-ghost btn-sm !px-2 !py-1"
                    :title="g.status === '在售' ? '下架' : '上架'"
                    :disabled="store.acting"
                    @click="toggleShelf(g)"
                  >
                    <i class="fa-solid" :class="g.status === '在售' ? 'fa-box-archive text-amber-500' : 'fa-arrow-up-from-bracket text-emerald-500'"></i>
                  </button>
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="编辑" @click="openEdit(g)">
                    <i class="fa-solid fa-pen"></i>
                  </button>
                  <button class="btn btn-ghost btn-sm !px-2 !py-1" title="删除" @click="removeGoods(g)">
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
      :fields="GOODS_FIELDS"
      :model-value="dialog.record"
      :loading="store.acting"
      @close="dialog.open = false"
      @submit="submitDialog"
    />
    <ConfirmDialog v-bind="confirmState" @resolve="resolveConfirm" />
  </div>
</template>
