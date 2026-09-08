import { defineStore } from 'pinia'

import {
  createAlarm,
  createNode,
  deleteAlarm,
  deleteNode,
  fetchClusterOverview,
  simulateCapacity,
  updateNode,
} from '@/api/cluster'
import { useAppStore } from './app'
import { useUiStore } from './ui'

/** 集群态势感知（页面 a6）视图模型。 */
export const useClusterStore = defineStore('cluster', {
  state: () => ({
    loading: false,
    error: '',
    data: null,
    acting: false,
  }),
  actions: {
    /** 统一执行：处理 loading / 成功提示 / 失败提示 / 自动刷新。 */
    async run(action, silent = false) {
      const ui = useUiStore()
      this.acting = true
      try {
        const result = await action()
        if (!silent && result?.message) ui.success(result.message)
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
        this.data = await fetchClusterOverview(app.rangeParams)
      } catch (error) {
        this.error = error.message
        ui.error(`集群大盘加载失败：${error.message}`)
      } finally {
        this.loading = false
      }
    },
    simulate() {
      return this.run(() => simulateCapacity())
    },
    createNode(payload) {
      return this.run(() => createNode(payload))
    },
    updateNode(payload) {
      return this.run(() => updateNode(payload))
    },
    removeNode(id) {
      return this.run(() => deleteNode(id))
    },
    createAlarm(payload) {
      return this.run(() => createAlarm(payload))
    },
    removeAlarm(id) {
      return this.run(() => deleteAlarm(id))
    },
  },
})
