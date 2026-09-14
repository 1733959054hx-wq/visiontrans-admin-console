import axios from 'axios'

import { isExtension, resolveApiBase } from './extConfig'

/**
 * axios 实例：统一超时与响应解包。
 *
 * 后端统一返回 { code, message, data, timestamp }，
 * 这里把 code === 200 的响应直接解包为 data，业务代码拿到的就是纯数据。
 *
 * baseURL 不写死：网页模式为相对 /api；扩展模式从本地配置实时解析绝对地址，
 * 因此放在请求拦截器里按请求设置，配置页改完地址立即生效。
 */
const request = axios.create({
  timeout: 10000,
})

// 请求头携带登录令牌
const TOKEN_KEY = 'admin_console_token'
request.interceptors.request.use((config) => {
  config.baseURL = resolveApiBase()
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers['X-Auth-Token'] = token
  }
  return config
})

/** 未登录 / 登录过期：清理本地令牌并跳转登录页（成功 / 失败两个分支共用） */
const redirectToLogin = () => {
  localStorage.removeItem(TOKEN_KEY)
  if (isExtension) {
    // hash 路由：只改 hash，不能 location.href='/login'（那会指向 chrome-extension 根下的真实路径）
    if (!location.hash.startsWith('#/login')) {
      location.hash = '#/login'
    }
    return
  }
  if (location.pathname !== '/login') {
    location.href = '/login'
  }
}

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && typeof body.code === 'number') {
      if (body.code === 200) {
        return body.data
      }
      // 关键：这里 reject 不会流入下面的 onRejected，401 必须就地处理
      if (body.code === 401) {
        redirectToLogin()
      }
      const err = new Error(body.message || `请求失败（${body.code}）`)
      err.code = body.code
      return Promise.reject(err)
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const code = error.response?.data?.code ?? error.code ?? status
    if (code === 401) {
      redirectToLogin()
    }
    const message = error.response?.data?.message || error.message || '网络异常'
    const err = new Error(message)
    err.code = code
    err.status = status
    return Promise.reject(err)
  },
)

export default request
