import { defineStore } from 'pinia'

import {
  adoptAdvice,
  createSlot,
  deleteSlot,
  fetchAdOverview,
  updateFrequency,
  updateSlot,
} from '@/api/ads'
import { useAppStore } from './app'
import { useUiStore } from './ui'

/** 广告位排期与调度引擎（页面 a9）视图模型。 */
export const useAdStore = defineStore('ads', {
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
        this.data = await fetchAdOverview(app.rangeParams)
      } catch (error) {
        this.error = error.message
        ui.error(`广告排期加载失败：${error.message}`)
      } finally {
        this.loading = false
      }
    },
    setFrequency(name, value) {
      return this.run(() => updateFrequency(name, value))
    },
    adopt() {
      return this.run(() => adoptAdvice())
    },
    createSlot(payload) {
      return this.run(() => createSlot(payload))
    },
    updateSlot(payload) {
      return this.run(() => updateSlot(payload))
    },
    removeSlot(id) {
      return this.run(() => deleteSlot(id))
    },
  },
})
