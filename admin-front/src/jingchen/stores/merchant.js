import { defineStore } from 'pinia'

import {
  createChannel,
  createGoods,
  createMaterial,
  createPlan,
  createVideo,
  deleteChannel,
  deleteGoods,
  deleteMaterial,
  deletePlan,
  deleteVideo,
  fetchAb,
  fetchChannels,
  fetchFunds,
  fetchGoods,
  fetchMaterials,
  fetchMerchantHome,
  fetchOnboard,
  fetchOrders,
  fetchOverview,
  fetchPlans,
  fetchProfile,
  fetchSales,
  fetchSlots,
  fetchVideos,
  pausePlan,
  rechargeFunds,
  resumePlan,
  saveAb,
  settleOrder as apiSettleOrder,
  shelfGoods,
  signContract,
  submitOnboard,
  unshelfGoods,
  updateChannel,
  updateGoods,
  updateMaterial,
  updatePlan,
  updateProfile,
  updateVideo,
  withdrawFunds,
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
    goods: null,
    goodsLoading: false,
    funds: null,
    fundsLoading: false,
  onboard: null,
  onboardLoading: false,
  slots: null,
  profile: null,
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
    async runOrder(action, reload) {
      const ui = useUiStore()
      this.acting = true
      try {
        const result = await action()
        if (result?.message) ui.success(result.message)
        if (reload === 'funds') await this.loadFunds()
        else await this.loadOrders()
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
    /* ------------------------------ 商品管理 ------------------------------ */
    async loadGoods() {
      const ui = useUiStore()
      this.goodsLoading = true
      try {
        this.goods = await fetchGoods()
      } catch (error) {
        ui.error(`商品加载失败：${error.message}`)
      } finally {
        this.goodsLoading = false
      }
    },
    async runGoods(action) {
      const ui = useUiStore()
      this.acting = true
      try {
        const result = await action()
        if (result?.message) ui.success(result.message)
        await this.loadGoods()
        return result
      } catch (error) {
        ui.error(error.message)
        throw error
      } finally {
        this.acting = false
      }
    },
    createGoods(payload) {
      return this.runGoods(() => createGoods(payload))
    },
    updateGoods(id, payload) {
      return this.runGoods(() => updateGoods(id, payload))
    },
    shelfGoods(id) {
      return this.runGoods(() => shelfGoods(id))
    },
    unshelfGoods(id) {
      return this.runGoods(() => unshelfGoods(id))
    },
    deleteGoods(id) {
      return this.runGoods(() => deleteGoods(id))
    },
    /* ------------------------------ 账户与资金 ------------------------------ */
    async loadFunds() {
      const ui = useUiStore()
      this.fundsLoading = true
      try {
        this.funds = await fetchFunds()
      } catch (error) {
        ui.error(`资金数据加载失败：${error.message}`)
      } finally {
        this.fundsLoading = false
      }
    },
    recharge(amount) {
      return this.runOrder(() => rechargeFunds(amount), 'funds')
    },
    withdraw(amount) {
      return this.runOrder(() => withdrawFunds(amount), 'funds')
    },
    /* ------------------------------ 入驻管理 ------------------------------ */
    async loadOnboard() {
      this.onboardLoading = true
      try {
        this.onboard = await fetchOnboard()
      } catch {
        this.onboard = null
      } finally {
        this.onboardLoading = false
      }
    },
    async submitOnboarding(payload) {
      const ui = useUiStore()
      this.acting = true
      try {
        this.onboard = await submitOnboard(payload)
        ui.success('入驻资质已提交，等待平台审核')
        return this.onboard
      } catch (error) {
        ui.error(error.message)
        throw error
      } finally {
        this.acting = false
      }
    },
    async signContract() {
      const ui = useUiStore()
      this.acting = true
      try {
        this.onboard = await signContract()
        ui.success('合作协议签署成功')
        return this.onboard
      } catch (error) {
        ui.error(error.message)
        throw error
      } finally {
        this.acting = false
      }
    },
    /* ------------------------------ 广告位与资料 ------------------------------ */
    async loadSlots() {
      if (this.slots) return
      try {
        this.slots = await fetchSlots()
      } catch {
        this.slots = []
      }
    },
    async loadProfile() {
      try {
        this.profile = await fetchProfile()
      } catch {
        this.profile = null
      }
    },
    async saveProfile(payload) {
      const ui = useUiStore()
      this.acting = true
      try {
        this.profile = await updateProfile(payload)
        ui.success('商户资料已更新')
        return this.profile
      } catch (error) {
        ui.error(error.message)
        throw error
      } finally {
        this.acting = false
      }
    },
  },
})
