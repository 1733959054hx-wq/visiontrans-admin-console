import { defineStore } from 'pinia'

let seq = 0

/**
 * 轻量全局提示（Toast）：替代原型里的 window.alert，所有异步操作结果统一走这里。
 */
export const useUiStore = defineStore('ui', {
  state: () => ({
    toasts: [],
  }),
  actions: {
    toast(message, type = 'success') {
      const id = ++seq
      this.toasts.push({ id, message, type })
      setTimeout(() => {
        this.toasts = this.toasts.filter((item) => item.id !== id)
      }, 2600)
    },
    success(message) {
      this.toast(message, 'success')
    },
    error(message) {
      this.toast(message, 'error')
    },
  },
})
