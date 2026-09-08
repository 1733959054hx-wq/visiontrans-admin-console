import request from '@/api/request'
import JSEncrypt from 'jsencrypt'

/**
 * 商户模块接口（jingchen 模块）。
 *
 * 商户走独立登录流程：/api/merchant/login 签发商户令牌，与后台 /api/auth/login 互不通用。
 * 该控制器标注 @RequireRole("MERCHANT")，管理员调用会被拒绝，两类用户数据互相隔离。
 */

/** 商户工作台首页（当前为占位数据，业务接口由后端组员补充） */
export function fetchMerchantHome() {
  return request.get('/merchant/home')
}

/** 获取 RSA 公钥（口令加密用，复用主工程公开端点，加密设施为共享工具） */
export async function getMerchantPublicKey() {
  const key = await request.get('/auth/public-key')
  return typeof key === 'string' ? key : key?.data ?? key
}

/**
 * 商户登录：口令先用 RSA 公钥加密再传输，与后台登录同套加密设施。
 *
 * @param username      登录账号（即商户编码）
 * @param password      口令明文（此处加密）
 * @param publicKey     后端下发的 RSA 公钥
 * @param captchaId     验证码挑战 ID（与后台共用验证码设施）
 * @param captchaClicks 按顺序点击的坐标 [{x, y}]
 */
export function merchantLogin(username, password, publicKey, captchaId, captchaClicks) {
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(publicKey)
  const encrypted = encryptor.encrypt(password)
  if (!encrypted) {
    return Promise.reject(new Error('口令加密失败，请刷新页面重试'))
  }
  return request.post('/merchant/login', { username, password: encrypted, captchaId, captchaClicks })
}

/** 商户登出 */
export function merchantLogout() {
  return request.post('/merchant/logout')
}

/** 获取当前登录商户身份 */
export function merchantMe() {
  return request.get('/merchant/me')
}
