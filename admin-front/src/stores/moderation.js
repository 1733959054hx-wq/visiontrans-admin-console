import { defineStore } from 'pinia'

import {
  createAsset,
  createPackage,
  createTask,
  decideUgc,
  deleteAsset,
  deletePackage,
  deleteTask,
  fetchModerationOverview,
  moveTask,
  processRefund,
  reviewAsset,
  reviewPackage,
  updateAsset,
  updatePackage,
  updateTask,
} from '@/api/moderation'
import { useAppStore } from './app'
import { useUiStore } from './ui'

/** 术语库审核与 UGC 风控中台（页面 a8）视图模型。 */
export const useModerationStore = defineStore('moderation', {
  state: () => ({
    loading: false,
    error: '',
    data: null,
    acting: false,
  }),
  actions: {
    async run(action) {
      const ui = useUiStore()
      this.acting = true
      try {
        const result = await action()
        if (result?.message) ui.success(result.message)
        await this.load()
        return result
      } catch (error) {
        ui.error(error.message)
        throw error
      } finally {
        this.acting = false
      }
    },
    async load() {
      const ui = useUiStore()
      const app = useAppStore()
      this.loading = true
      this.error = ''
      try {
        this.data = await fetchModerationOverview(app.rangeParams)
      } catch (error) {
        this.error = error.message
        ui.error(`风控中台加载失败：${error.message}`)
      } finally {
        this.loading = false
      }
    },
    decide(action) {
      return this.run(() => decideUgc(action))
    },
    refund() {
      return this.run(() => processRefund())
    },
    createTask(payload) {
      return this.run(() => createTask(payload))
    },
    updateTask(payload) {
      return this.run(() => updateTask(payload))
    },
    moveTask(id, direction) {
      return this.run(() => moveTask(id, direction))
    },
    removeTask(id) {
      return this.run(() => deleteTask(id))
    },
    createAsset(payload) {
      return this.run(() => createAsset(payload))
    },
    updateAsset(payload) {
      return this.run(() => updateAsset(payload))
    },
    removeAsset(id) {
      return this.run(() => deleteAsset(id))
    },
    /** 素材人工复审（decision = pass / reject）。 */
    reviewAsset(id, decision) {
      return this.run(() => reviewAsset(id, decision))
    },
    createPackage(payload) {
      return this.run(() => createPackage(payload))
    },
    updatePackage(payload) {
      return this.run(() => updatePackage(payload))
    },
    removePackage(id) {
      return this.run(() => deletePackage(id))
    },
    /** 知识包审核（decision = pass / reject）。 */
    reviewPackage(id, decision) {
      return this.run(() => reviewPackage(id, decision))
    },
  },
})
