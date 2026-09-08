import { defineStore } from 'pinia'
import { login as apiLogin, logout as apiLogout, me as apiMe } from '@/api/auth'
import { useUiStore } from './ui'

const TOKEN_KEY = 'admin_console_token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null,
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
  },
  actions: {
    setToken(token) {
      this.token = token || ''
      if (this.token) localStorage.setItem(TOKEN_KEY, this.token)
      else localStorage.removeItem(TOKEN_KEY)
    },
    async login(username, password, publicKey, captchaId, captchaClicks) {
      const res = await apiLogin(username, password, publicKey, captchaId, captchaClicks)
      this.setToken(res.token)
      this.user = res.profile
      return res
    },
    /** 应用启动时用本地令牌恢复登录态 */
    async restore() {
      if (!this.token) return
      try {
        this.user = await apiMe()
      } catch {
        this.setToken('')
        this.user = null
      }
    },
    async logout() {
      try {
        await apiLogout()
      } catch {
        /* 即使后端失败也本地清理 */
      }
      this.setToken('')
      this.user = null
      const ui = useUiStore()
      ui.success('已退出登录')
    },
  },
})
