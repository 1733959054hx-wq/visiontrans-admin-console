import { defineStore } from 'pinia'

import {
  fetchFinanceOverview,
  handleOrder,
  reconcileSettlement,
  reviewInvoice,
  reviewOnboarding,
  settleSettlement,
  signOnboarding,
} from '@/api/finance'
import { useAppStore } from './app'
import { useUiStore } from './ui'

/** 财务订单与商户结算中心（页面 a11）视图模型。 */
export const useFinanceStore = defineStore('finance', {
  state: () => ({
    loading: false,
    error: '',
    data: null,
    acting: false,
  }),
  actions: {
    /** 统一执行：处理成功提示 / 失败提示 / 自动刷新大盘。 */
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
        this.data = await fetchFinanceOverview(app.rangeParams)
      } catch (error) {
        this.error = error.message
        ui.error(`财务数据加载失败：${error.message}`)
      } finally {
        this.loading = false
      }
    },
    /** 订单处置（markPaid / refund / close）。 */
    handle(id, action) {
      return this.run(() => handleOrder(id, action))
    },
    reconcile(id) {
      return this.run(() => reconcileSettlement(id))
    },
    settle(id) {
      return this.run(() => settleSettlement(id))
    },
    /** 发票审核（approved = true / false）。 */
    review(id, approved) {
      return this.run(() => reviewInvoice(id, approved))
    },
    /** 商户入驻审核（approved = true 通过 / false 驳回）。 */
    reviewOnboarding(id, approved) {
      return this.run(() => reviewOnboarding(id, approved))
    },
    /** 标记平台合作协议已签署。 */
    signOnboarding(id) {
      return this.run(() => signOnboarding(id))
    },
  },
})
