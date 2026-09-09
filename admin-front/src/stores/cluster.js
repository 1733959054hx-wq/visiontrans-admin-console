import { defineStore } from 'pinia'

import {
  createAlarm,
  createNode,
  deleteAlarm,
  deleteNode,
  fetchClusterOps,
  fetchClusterOverview,
  fetchClusterSysLogs,
  fetchDependencies,
  probeAllDependencies,
  probeDependency,
  simulateCapacity,
  updateBackup,
  updateBreaker,
  updateNode,
  updateThresholds,
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
    /** 运维管理数据：熔断降级 / 备份策略 / 告警阈值 */
    ops: null,
    /** 系统日志检索结果 { logs: [...] } */
    sysLogs: null,
    logsLoading: false,
    /** 核心服务与第三方接口可用性 { rows, probeMode, updatedAt } */
    dependencies: null,
    depsLoading: false,
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
      // 并发保护：登录成功后会预取大盘，随后 TopBar 与集群页挂载又会各调一次；
      // 没有这个判断会同时发出多份相同的聚合请求（该接口较重，重复查询代价明显）。
      if (this.loading) return
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
    /* ------------------------------ 运维管理 / 系统日志 ------------------------------ */
    /** 拉取运维管理数据（熔断 / 备份 / 阈值），独立于主大盘。 */
    async loadOps() {
      try {
        this.ops = await fetchClusterOps()
      } catch {
        // 辅助区块：失败静默，保留空态
      }
    },
    /** 按级别 / 分类检索系统日志（均可空）。 */
    async loadSysLogs(params = {}) {
      this.logsLoading = true
      try {
        this.sysLogs = await fetchClusterSysLogs(params)
      } catch (error) {
        useUiStore().error(`系统日志加载失败：${error.message}`)
      } finally {
        this.logsLoading = false
      }
    },
    /** 更新熔断器状态与启用，成功后回读运维数据。 */
    async setBreaker(payload) {
      const result = await this.run(() => updateBreaker(payload))
      await this.loadOps()
      return result
    },
    /** 更新备份策略启用，成功后回读运维数据。 */
    async setBackup(payload) {
      const result = await this.run(() => updateBackup(payload))
      await this.loadOps()
      return result
    },
    /** 更新资源告警阈值（cpu / mem / gpu），成功后回读运维数据。 */
    async setThresholds(cpu, mem, gpu) {
      const result = await this.run(() => updateThresholds(cpu, mem, gpu))
      await this.loadOps()
      return result
    },
    /* ---------------------- 核心服务与第三方接口可用性 ---------------------- */
    /** 拉取依赖服务可用性列表。 */
    async loadDependencies() {
      this.depsLoading = true
      try {
        this.dependencies = await fetchDependencies()
      } catch (error) {
        useUiStore().error(`依赖服务加载失败：${error.message}`)
      } finally {
        this.depsLoading = false
      }
    },
    /** 对单个依赖服务发起真实拨测，成功后回读列表。 */
    async probe(id) {
      const result = await this.run(() => probeDependency(id))
      await this.loadDependencies()
      return result
    },
    /** 一键拨测全部依赖服务。 */
    async probeAll() {
      const result = await this.run(() => probeAllDependencies())
      await this.loadDependencies()
      return result
    },
  },
})
