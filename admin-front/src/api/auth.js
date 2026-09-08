import request from './request'
import JSEncrypt from 'jsencrypt'

/** 获取点击式图形验证码挑战：{ id, image, hint } */
export function getCaptcha() {
  return request.get('/auth/captcha')
}

/** 获取 RSA 公钥（用于加密登录口令） */
export async function getPublicKey() {
  const key = await request.get('/auth/public-key')
  // request 层对纯字符串响应可能不做解包，这里统一兜底
  return typeof key === 'string' ? key : key?.data ?? key
}

/**
 * 登录：口令先用 RSA 公钥加密再传输。
 *
 * @param username      登录账号
 * @param password      口令明文（此处加密）
 * @param publicKey     后端下发的 RSA 公钥
 * @param captchaId     验证码挑战 ID
 * @param captchaClicks 按顺序点击的坐标 [{x, y}]
 */
export function login(username, password, publicKey, captchaId, captchaClicks) {
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(publicKey)
  const encrypted = encryptor.encrypt(password)
  if (!encrypted) {
    return Promise.reject(new Error('口令加密失败，请刷新页面重试'))
  }
  return request.post('/auth/login', { username, password: encrypted, captchaId, captchaClicks })
}

/** 登出 */
export function logout() {
  return request.post('/auth/logout')
}

/** 获取当前登录管理员信息 */
export function me() {
  return request.get('/auth/me')
}
