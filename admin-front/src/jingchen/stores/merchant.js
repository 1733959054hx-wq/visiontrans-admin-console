import { defineStore } from 'pinia'

import { fetchMerchantHome } from '@/jingchen/api/merchant'
import { useUiStore } from '@/stores/ui'

/** 商户工作台状态（模块自有 store，不复用主工程业务 store）。 */
export const useMerchantStore = defineStore('jingchenMerchant', {
  state: () => ({
    loading: false,
    error: '',
    home: null,
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
  },
})
