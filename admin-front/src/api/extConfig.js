/**
 * 浏览器扩展（chrome-extension://）与普通网页两种运行形态的统一适配。
 *
 * 网页模式：接口走相对路径 /api（前后端同域或 Vite 代理），路由为 history；
 * 扩展模式：接口必须指向可访问的后端绝对地址（首次打开时由用户在配置页填写），
 *           路由为 hash（chrome-extension 协议下 history 路由不可用）。
 */

/** 是否以浏览器扩展形态运行（vite build --mode extension 时成立）。 */
export const isExtension = import.meta.env.MODE === 'extension'

/** 扩展模式下后端地址的本地存储键。 */
export const EXT_API_BASE_KEY = 'visiontrans_ext_api_base'

/** 首次配置页的默认值：本机开发环境的 Spring Boot 地址。 */
export const DEFAULT_EXT_API_BASE = 'http://localhost:8080/api'

/**
 * 当前应使用的 API 基址。
 * 扩展模式每次实时读取，用户在配置页修改后无需刷新即可生效；
 * 网页模式保持构建期注入的相对路径。
 */
export function resolveApiBase() {
  if (!isExtension) {
    return import.meta.env.VITE_API_BASE_URL || '/api'
  }
  return normalizeApiBase(localStorage.getItem(EXT_API_BASE_KEY)) || DEFAULT_EXT_API_BASE
}

/** 扩展是否已完成后端地址配置（未配置时由路由守卫拦到配置页）。 */
export function hasExtensionApiBase() {
  return !!normalizeApiBase(localStorage.getItem(EXT_API_BASE_KEY))
}

export function saveExtensionApiBase(value) {
  const normalized = normalizeApiBase(value)
  if (!normalized) return ''
  localStorage.setItem(EXT_API_BASE_KEY, normalized)
  return normalized
}

export function getSavedExtensionApiBase() {
  return normalizeApiBase(localStorage.getItem(EXT_API_BASE_KEY))
}

/**
 * 规整用户输入：去空白、去末尾斜杠；只接受 http(s) 绝对地址。
 * 自动补 /api 后缀（用户一般只记得填到端口，如 http://localhost:8080）。
 */
export function normalizeApiBase(value) {
  if (!value) return ''
  let v = String(value).trim().replace(/\/+$/, '')
  if (!/^https?:\/\/.+/i.test(v)) return ''
  if (!/\/api$/i.test(v)) v += '/api'
  return v
}
