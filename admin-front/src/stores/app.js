import { defineStore } from 'pinia'

import { fetchNav, fetchSystem } from '@/api/meta'
import { useAdStore } from './ads'
import { useClusterStore } from './cluster'
import { useFinanceStore } from './finance'
import { useModelStore } from './models'
import { useModerationStore } from './moderation'
import { useSecurityStore } from './security'

const pad = (n) => String(n).padStart(2, '0')
const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const today = () => fmt(new Date())
const monthStart = () => {
  const d = new Date()
  return fmt(new Date(d.getFullYear(), d.getMonth(), 1))
}

/**
 * 应用级全局状态：导航菜单、系统信息、侧栏折叠。
 * 对应原型里的 NAV / STATE.mode / STATE.side。
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    /** 导航菜单：按一级页面分组下发，已由后端按当前角色过滤 */
    nav: { groups: [], roleCode: '' },
    system: null,
    sideCollapsed: false,
    metaLoading: false,
    /** 顶栏全局搜索关键字：各页面表格按此过滤 */
    keyword: '',
    /** 帮助弹窗开关 */
    helpOpen: false,
    /** 全局日期范围（yyyy-MM-dd）：各页面大盘按此过滤数据 */
    dateStart: monthStart(),
    dateEnd: today(),
    /** 日期范围版本号：变更时 +1，用于联动刷新 */
    rangeVersion: 0,
  }),
  getters: {
    /** 全部菜单项拍平（快捷切换、面包屑等场景）。 */
    navItems: (state) => (state.nav.groups || []).flatMap((group) => group.items || []),
    /** 按路由名取菜单项元数据。 */
    metaOf: (state) => (id) =>
      (state.nav.groups || []).flatMap((group) => group.items || []).find((item) => item.id === id) || null,
    /** 指定路由名是否出现在当前角色可见的菜单中（路由守卫兜底用）。 */
    navContains: (state) => (id) =>
      (state.nav.groups || []).some((group) => (group.items || []).some((item) => item.id === id)),
    /** 大盘接口的日期范围查询参数（任一为空时不限）。 */
    rangeParams: (state) =>
      state.dateStart && state.dateEnd ? { start: state.dateStart, end: state.dateEnd } : {},
  },
  actions: {
    async loadMeta() {
      this.metaLoading = true
      try {
        const [nav, system] = await Promise.all([fetchNav(), fetchSystem()])
        this.nav = nav
        this.system = system
      } catch {
        // 公开页（未登录）请求 meta 会 401：静默忽略，登录成功后再正常加载
      } finally {
        this.metaLoading = false
      }
    },
    /**
     * 设置全局日期范围（yyyy-MM-dd），并联动刷新所有已加载的大盘数据。
     * @param {string} start 起始日
     * @param {string} end 结束日
     * @param {'start'|'end'} changed 本次改动的是哪一端，用于自动收敛非法范围
     */
    setRange(start, end, changed = 'start') {
      if (!start || !end) return
      if (start > end) {
        // 起始日晚于结束日时，把另一端拖到同一天，避免出现"永远查不到数据"的空范围
        if (changed === 'end') this.dateStart = end
        else this.dateEnd = start
      } else {
        this.dateStart = start
        this.dateEnd = end
      }
      this.rangeVersion++
      // 已加载过的页面立即按新范围重新拉取；统一回到第 1 页，避免停留在超出新总页数的页码
      const loaded = [useClusterStore(), useModelStore(), useModerationStore(), useAdStore(), useSecurityStore(), useFinanceStore()]
      loaded.forEach((store) => {
        if (store.data) store.load(1)
      })
    },
    toggleSide() {
      this.sideCollapsed = !this.sideCollapsed
    },
    setKeyword(keyword) {
      this.keyword = keyword || ''
    },
    toggleHelp(open) {
      this.helpOpen = open === undefined ? !this.helpOpen : open
    },
  },
})
