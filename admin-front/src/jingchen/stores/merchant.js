import { defineStore } from 'pinia'

import {
  createChannel,
  createMaterial,
  createPlan,
  createVideo,
  deleteChannel,
  deleteMaterial,
  deletePlan,
  deleteVideo,
  fetchAb,
  fetchChannels,
  fetchMaterials,
  fetchMerchantHome,
  fetchOrders,
  fetchOverview,
  fetchPlans,
  fetchSales,
  fetchVideos,
  pausePlan,
  resumePlan,
  saveAb,
  settleOrder as apiSettleOrder,
  updateChannel,
  updateMaterial,
  updatePlan,
  updateVideo,
} from '@/jingchen/api/merchant'
import { useUiStore } from '@/stores/ui'

/** 商户工作台状态（模块自有 store，不复用主工程业务 store）。 */
export const useMerchantStore = defineStore('jingchenMerchant', {
  state: () => ({
    loading: false,
    error: '',
    home: null,
    plans: null,
    plansLoading: false,
    acting: false,
    overview: null,
    overviewLoading: false,
    orders: null,
    ordersLoading: false,
    materials: null,
    materialsLoading: false,
    ab: null,
    videos: null,
    videosLoading: false,
    channels: null,
    channelsLoading: false,
    sales: null,
    salesLoading: false,
  }),
  actions: {
    async loadHome() {
      const ui = useUiStore()
      this.loading = true
      this.error = ''
      try {
        this.home = await fetchMerchantHome()
      } catch (error) {
        this.error = error.message
        ui.error(`商户工作台加载失败：${error.message}`)
      } finally {
        this.loading = false
      }
    },
    /* ------------------------------ 投放计划 ------------------------------ */
    async loadPlans() {
      const ui = useUiStore()
      this.plansLoading = true
      this.error = ''
      try {
        this.plans = await fetchPlans()
      } catch (error) {
        this.error = error.message
        ui.error(`投放计划加载失败：${error.message}`)
      } finally {
        this.plansLoading = false
      }
    },
    /** 执行写操作并刷新列表：store 统一处理成功提示与失败报错 */
    async runPlan(action) {
      const ui = useUiStore()
      this.acting = true
      try {
        const result = await action()
        if (result?.message) ui.success(result.message)
        await this.loadPlans()
        return result
      } catch (error) {
        ui.error(error.message)
        throw error
      } finally {
        this.acting = false
      }
    },
    createPlan(payload) {
      return this.runPlan(() => createPlan(payload))
    },
    updatePlan(id, payload) {
      return this.runPlan(() => updatePlan(id, payload))
    },
    pausePlan(id) {
      return this.runPlan(() => pausePlan(id))
    },
    resumePlan(id) {
      return this.runPlan(() => resumePlan(id))
    },
    deletePlan(id) {
      return this.runPlan(() => deletePlan(id))
    },
    /* ------------------------------ 经营概览 ------------------------------ */
    async loadOverview() {
      const ui = useUiStore()
      this.overviewLoading = true
      try {
        this.overview = await fetchOverview()
      } catch (error) {
        ui.error(`经营概览加载失败：${error.message}`)
      } finally {
        this.overviewLoading = false
      }
    },
    /* ------------------------------ 订单与结算 ------------------------------ */
    async loadOrders() {
      const ui = useUiStore()
      this.ordersLoading = true
      try {
        this.orders = await fetchOrders()
      } catch (error) {
        ui.error(`订单加载失败：${error.message}`)
      } finally {
        this.ordersLoading = false
      }
    },
    async runOrder(action) {
      const ui = useUiStore()
      this.acting = true
      try {
        const result = await action()
        if (result?.message) ui.success(result.message)
        await this.loadOrders()
        return result
      } catch (error) {
        ui.error(error.message)
        throw error
      } finally {
        this.acting = false
      }
    },
    settleOrder(id) {
      return this.runOrder(() => apiSettleOrder(id))
    },
    /* ------------------------------ 素材管理(含 A/B) ------------------------------ */
    async loadMaterials() {
      const ui = useUiStore()
      this.materialsLoading = true
      try {
        this.materials = await fetchMaterials()
      } catch (error) {
        ui.error(`素材加载失败：${error.message}`)
      } finally {
        this.materialsLoading = false
      }
    },
    async loadAb() {
      try {
        this.ab = await fetchAb()
      } catch {
        this.ab = null
      }
    },
    async runMaterial(action, reloadAb) {
      const ui = useUiStore()
      this.acting = true
      try {
        const result = await action()
        if (result?.message) ui.success(result.message)
        await this.loadMaterials()
        if (reloadAb) await this.loadAb()
        return result
      } catch (error) {
        ui.error(error.message)
        throw error
      } finally {
        this.acting = false
      }
    },
    createMaterial(payload) {
      return this.runMaterial(() => createMaterial(payload))
    },
    updateMaterial(id, payload) {
      return this.runMaterial(() => updateMaterial(id, payload))
    },
    deleteMaterial(id) {
      return this.runMaterial(() => deleteMaterial(id))
    },
    saveAbConfig(payload) {
      return this.runMaterial(() => saveAb(payload), true)
    },
    /* ------------------------------ 视频接入 ------------------------------ */
    async loadVideos() {
      const ui = useUiStore()
      this.videosLoading = true
      try {
        this.videos = await fetchVideos()
      } catch (error) {
        ui.error(`视频加载失败：${error.message}`)
      } finally {
        this.videosLoading = false
      }
    },
    async runVideo(action) {
      const ui = useUiStore()
      this.acting = true
      try {
        const result = await action()
        if (result?.message) ui.success(result.message)
        await this.loadVideos()
        return result
      } catch (error) {
        ui.error(error.message)
        throw error
      } finally {
        this.acting = false
      }
    },
    createVideo(payload) {
      return this.runVideo(() => createVideo(payload))
    },
    updateVideo(id, payload) {
      return this.runVideo(() => updateVideo(id, payload))
    },
    deleteVideo(id) {
      return this.runVideo(() => deleteVideo(id))
    },
    /* ------------------------------ 推广渠道与销售报表 ------------------------------ */
    async loadChannels() {
      const ui = useUiStore()
      this.channelsLoading = true
      try {
        this.channels = await fetchChannels()
      } catch (error) {
        ui.error(`推广渠道加载失败：${error.message}`)
      } finally {
        this.channelsLoading = false
      }
    },
    async loadSales() {
      const ui = useUiStore()
      this.salesLoading = true
      try {
        this.sales = await fetchSales()
      } catch (error) {
        ui.error(`销售报表加载失败：${error.message}`)
      } finally {
        this.salesLoading = false
      }
    },
    async runChannel(action) {
      const ui = useUiStore()
      this.acting = true
      try {
        const result = await action()
        if (result?.message) ui.success(result.message)
        await this.loadChannels()
        return result
      } catch (error) {
        ui.error(error.message)
        throw error
      } finally {
        this.acting = false
      }
    },
    createChannel(payload) {
      return this.runChannel(() => createChannel(payload))
    },
    updateChannel(id, payload) {
      return this.runChannel(() => updateChannel(id, payload))
    },
    deleteChannel(id) {
      return this.runChannel(() => deleteChannel(id))
    },
  },
})
