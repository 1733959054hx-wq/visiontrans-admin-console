import axios from 'axios'

/**
 * axios 实例：统一 baseURL、超时与响应解包。
 *
 * 后端统一返回 { code, message, data, timestamp }，
 * 这里把 code === 200 的响应直接解包为 data，业务代码拿到的就是纯数据。
 */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
})

// 请求头携带登录令牌
const TOKEN_KEY = 'admin_console_token'
request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers['X-Auth-Token'] = token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && typeof body.code === 'number') {
      if (body.code === 200) {
        return body.data
      }
      // 把业务码（如 401 未登录）带在错误对象上，供下面统一处理跳转
      const err = new Error(body.message || `请求失败（${body.code}）`)
      err.code = body.code
      return Promise.reject(err)
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const code = error.code ?? status
    // 未登录 / 登录过期：清理本地令牌并跳转登录页
    if (code === 401) {
      localStorage.removeItem(TOKEN_KEY)
      if (location.pathname !== '/login') {
        location.href = '/login'
      }
    }
    const message = error.response?.data?.message || error.message || '网络异常'
    return Promise.reject(new Error(status ? `${message}（HTTP ${status}）` : message))
  },
)

export default request
