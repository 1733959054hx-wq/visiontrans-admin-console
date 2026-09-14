import { defineStore } from 'pinia'
import { ref } from 'vue'

import { unifiedLogin, logout as apiLogout, me as apiMe } from '@/api/auth'
import { merchantLogout, merchantMe } from '@/jingchen/api/merchant'

/** 登录身份持久化键：刷新页面后能据此分辨调用哪套 /me 与 /logout */
const IDENTITY_KEY = 'admin_console_identity'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('admin_console_token') || '',
    user: null,
    // 'admin' | 'merchant'：决定 /me、/logout 走哪套接口，以及登录后落地页
    identity: localStorage.getItem(IDENTITY_KEY) || '',
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    /** 当前角色编码（SUPER_ADMIN / OPERATIONS / AUDITOR / MERCHANT） */
    roleCode: (s) => s.user?.roleCode || '',
    /** 是否商户用户：决定进入哪套页面、能访问哪些接口 */
    isMerchant: (s) => s.identity === 'MERCHANT' || s.user?.roleCode === 'MERCHANT',
  },
  actions: {
    setToken(token) {
      this.token = token
      if (token) localStorage.setItem('admin_console_token', token)
      else localStorage.removeItem('admin_console_token')
    },
    /** 记录登录身份（admin / merchant），供 restore 与 logout 区分接口 */
    setIdentity(identity) {
      this.identity = identity
      if (identity) localStorage.setItem(IDENTITY_KEY, identity)
      else localStorage.removeItem(IDENTITY_KEY)
    },
    /**
     * 统一登录：后端按账号自动识别身份（管理员优先），返回 identity('admin'/'merchant')。
     * 两类令牌存于同一 localStorage 键，拦截器按令牌所属会话体系区分，
     * restore / logout 再按这里记录的 identity 各走各的 /me、/logout。
     */
    async login(username, password, publicKey, captchaId, captchaClicks) {
      const res = await unifiedLogin(username, password, publicKey, captchaId, captchaClicks)
      this.setToken(res.token)
      this.setIdentity(res.identity === 'merchant' ? 'merchant' : 'admin')
      this.user = res.profile
      return res
    },
    async restore() {
      if (!this.token) return
      try {
        // 按身份调用对应接口：商户与后台会话表分离，必须各走各的 /me
        this.user = this.identity === 'merchant' ? await merchantMe() : await apiMe()
      } catch {
        this.setToken('')
        this.setIdentity('')
        this.user = null
      }
    },
    async logout() {
      try {
        if (this.isMerchant) await merchantLogout()
        else await apiLogout()
      } catch {
        // 忽略接口报错，仍然清理本地状态
      }
      this.setToken('')
      this.setIdentity('')
      this.user = null
    },
  },
})
