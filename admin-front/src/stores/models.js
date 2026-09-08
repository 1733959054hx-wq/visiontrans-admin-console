import { defineStore } from 'pinia'

import {
  createModel,
  deleteModel,
  fetchModelOverview,
  hotUpdateModel,
  rollbackModel,
  updateGrayscale,
  updateModel,
  updateStrategy,
} from '@/api/models'
import { useAppStore } from './app'
import { useUiStore } from './ui'

/** AI 模型生命周期与热更中心（页面 a7）视图模型。 */
export const useModelStore = defineStore('models', {
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
        this.data = await fetchModelOverview(app.rangeParams)
      } catch (error) {
        this.error = error.message
        ui.error(`模型中心加载失败：${error.message}`)
      } finally {
        this.loading = false
      }
    },
    setGrayscale(ratio) {
      return this.run(() => updateGrayscale(ratio))
    },
    hotUpdate(name) {
      return this.run(() => hotUpdateModel(name))
    },
    rollback(name) {
      return this.run(() => rollbackModel(name))
    },
    create(payload) {
      return this.run(() => createModel(payload))
    },
    update(payload) {
      return this.run(() => updateModel(payload))
    },
    remove(name) {
      return this.run(() => deleteModel(name))
    },
    setStrategy(name, enabled) {
      return this.run(() => updateStrategy(name, enabled))
    },
  },
})
