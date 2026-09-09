import { defineStore } from 'pinia'

import {
  banDevice,
  createAdmin,
  createDevice,
  createPlan,
  deleteAdmin,
  deleteDevice,
  deletePlan,
  fetchSecurityOverview,
  fetchSysConfig,
  updateAdmin,
  updateDevice,
  updateFeature,
  updatePermission,
  updatePlan,
  updatePolicy,
  updateSysParam,
  updateUser,
} from '@/api/security'
import { useAppStore } from './app'
import { useUiStore } from './ui'

/** 安全风控、设备审计与 RBAC 权限（页面 a10）视图模型。 */
export const useSecurityStore = defineStore('security', {
  state: () => ({
    loading: false,
    error: '',
    data: null,
    acting: false,
    /** 审计日志当前页码 */
    page: 1,
    /** 系统配置（运行参数 + 功能开关） */
    sysConfig: null,
  }),
  actions: {
    /**
     * 统一执行：处理 loading / 成功提示 / 失败提示 / 自动刷新。
     * @param {Function} action 请求函数
     * @param {boolean} reload 为 false 时跳过自动刷新（批量操作由调用方统一刷新一次）
     */
    async run(action, reload = true) {
      const ui = useUiStore()
      this.acting = true
      try {
        const result = await action()
        if (result?.message) ui.success(result.message)
        if (reload) await this.load(this.page)
        return result
      } catch (error) {
        ui.error(error.message)
        throw error
      } finally {
        this.acting = false
      }
    },
    async load(page = 1) {
      const ui = useUiStore()
      const app = useAppStore()
      this.loading = true
      this.error = ''
      this.page = page
      try {
        this.data = await fetchSecurityOverview(page, app.rangeParams)
      } catch (error) {
        this.error = error.message
        ui.error(`安全风控加载失败：${error.message}`)
      } finally {
        this.loading = false
      }
    },
    ban(fingerprint) {
      return this.run(() => banDevice(fingerprint))
    },
    createDevice(payload) {
      return this.run(() => createDevice(payload))
    },
    updateDevice(payload) {
      return this.run(() => updateDevice(payload))
    },
    removeDevice(fingerprint) {
      return this.run(() => deleteDevice(fingerprint))
    },
    createPlan(payload) {
      return this.run(() => createPlan(payload))
    },
    updatePlan(payload) {
      return this.run(() => updatePlan(payload))
    },
    removePlan(name) {
      return this.run(() => deletePlan(name))
    },
    createAdmin(payload) {
      return this.run(() => createAdmin(payload))
    },
    updateAdmin(payload) {
      return this.run(() => updateAdmin(payload))
    },
    removeAdmin(id) {
      return this.run(() => deleteAdmin(id))
    },
    setPolicy(name, enabled) {
      return this.run(() => updatePolicy(name, enabled))
    },
    savePermission(payload) {
      return this.run(() => updatePermission(payload))
    },
    /** 更新 C 端用户（会员状态 / 账号状态）。 */
    updateUser(payload) {
      return this.run(() => updateUser(payload))
    },
    /** 拉取系统配置（参数 / 功能开关），独立于大盘数据。 */
    async loadSysConfig() {
      try {
        this.sysConfig = await fetchSysConfig()
      } catch {
        // 系统配置为辅助区块：失败静默，避免干扰主页面
      }
    },
    /** 更新单个系统参数（value 为数字字符串），成功后回读最新配置。 */
    setSysParam(name, value) {
      return this.run(async () => {
        const result = await updateSysParam(name, value)
        await this.loadSysConfig()
        return result
      }, false)
    },
    /** 切换单个功能开关，成功后回读最新配置。 */
    setFeature(name, enabled) {
      return this.run(async () => {
        const result = await updateFeature(name, enabled)
        await this.loadSysConfig()
        return result
      }, false)
    },
    /** 批量保存权限（只刷新一次，避免逐条请求重复拉取大盘） */
    savePermissionBatch(payloads) {
      return this.run(async () => {
        for (const payload of payloads) {
          await updatePermission(payload)
        }
        return { message: `已保存 ${payloads.length} 项权限配置` }
      })
    },
  },
})
