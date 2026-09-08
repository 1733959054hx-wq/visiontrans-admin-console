import { defineStore } from 'pinia'
import { ref } from 'vue'

import { login as apiLogin, logout as apiLogout, me as apiMe } from '@/api/auth'
import { merchantLogin, merchantLogout, merchantMe } from '@/jingchen/api/merchant'

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
    async login(username, password, publicKey, captchaId, captchaClicks) {
      const res = await apiLogin(username, password, publicKey, captchaId, captchaClicks)
      this.setToken(res.token)
      this.setIdentity('admin')
      this.user = res.profile
      return res
    },
    /** 商户独立登录：签发商户令牌，存于同一 localStorage 键，由拦截器按令牌类型区分 */
    async loginMerchant(username, password, publicKey, captchaId, captchaClicks) {
      const res = await merchantLogin(username, password, publicKey, captchaId, captchaClicks)
      this.setToken(res.token)
      this.setIdentity('merchant')
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
