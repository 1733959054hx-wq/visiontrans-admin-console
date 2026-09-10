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

/* ------------------------------ 投放计划 CRUD ------------------------------ */

/** 投放计划列表 */
export function fetchPlans() {
  return request.get('/merchant/plans')
}

/** 新建投放计划 */
export function createPlan(payload) {
  return request.post('/merchant/plans', payload)
}

/** 编辑投放计划 */
export function updatePlan(id, payload) {
  return request.put(`/merchant/plans/${id}`, payload)
}

/** 暂停投放 */
export function pausePlan(id) {
  return request.put(`/merchant/plans/${id}/pause`)
}

/** 恢复投放 */
export function resumePlan(id) {
  return request.put(`/merchant/plans/${id}/resume`)
}

/** 删除投放计划 */
export function deletePlan(id) {
  return request.delete(`/merchant/plans/${id}`)
}

/* ------------------------------ 经营概览 ------------------------------ */

/** 经营概览:指标卡 + 近 7 日趋势 + 订单摘要 */
export function fetchOverview() {
  return request.get('/merchant/overview')
}

/* ------------------------------ 订单与结算 ------------------------------ */

/** 订单列表(最新在前) */
export function fetchOrders() {
  return request.get('/merchant/orders')
}

/** 结算一笔待结算订单 */
export function settleOrder(id) {
  return request.put(`/merchant/orders/${id}/settle`)
}

/* ------------------------------ 素材管理(含 A/B) ------------------------------ */

export function fetchMaterials() {
  return request.get('/merchant/materials')
}
export function createMaterial(payload) {
  return request.post('/merchant/materials', payload)
}
export function updateMaterial(id, payload) {
  return request.put(`/merchant/materials/${id}`, payload)
}
export function deleteMaterial(id) {
  return request.delete(`/merchant/materials/${id}`)
}
export function fetchAb() {
  return request.get('/merchant/materials/ab')
}
export function saveAb(payload) {
  return request.put('/merchant/materials/ab', payload)
}

/* ------------------------------ 视频接入 ------------------------------ */

export function fetchVideos() {
  return request.get('/merchant/videos')
}
export function createVideo(payload) {
  return request.post('/merchant/videos', payload)
}
export function updateVideo(id, payload) {
  return request.put(`/merchant/videos/${id}`, payload)
}
export function deleteVideo(id) {
  return request.delete(`/merchant/videos/${id}`)
}

/* ------------------------------ 推广渠道与销售报表 ------------------------------ */

export function fetchChannels() {
  return request.get('/merchant/promotions')
}
export function createChannel(payload) {
  return request.post('/merchant/promotions', payload)
}
export function updateChannel(id, payload) {
  return request.put(`/merchant/promotions/${id}`, payload)
}
export function deleteChannel(id) {
  return request.delete(`/merchant/promotions/${id}`)
}
export function fetchSales() {
  return request.get('/merchant/sales')
}

/* ------------------------------ 商品管理 ------------------------------ */

export function fetchGoods() {
  return request.get('/merchant/goods')
}
export function createGoods(payload) {
  return request.post('/merchant/goods', payload)
}
export function updateGoods(id, payload) {
  return request.put(`/merchant/goods/${id}`, payload)
}
export function shelfGoods(id) {
  return request.put(`/merchant/goods/${id}/shelf`)
}
export function unshelfGoods(id) {
  return request.put(`/merchant/goods/${id}/unshelf`)
}
export function deleteGoods(id) {
  return request.delete(`/merchant/goods/${id}`)
}

/* ------------------------------ 账户与资金 ------------------------------ */

export function fetchFunds() {
  return request.get('/merchant/funds')
}
export function rechargeFunds(amount) {
  return request.post('/merchant/funds/recharge', { amount })
}
export function withdrawFunds(amount) {
  return request.post('/merchant/funds/withdraw', { amount })
}

/* ------------------------------ 入驻管理 ------------------------------ */

export function fetchOnboard() {
  return request.get('/merchant/onboarding')
}
export function submitOnboard(payload) {
  return request.post('/merchant/onboarding/submit', payload)
}
export function signContract() {
  return request.post('/merchant/onboarding/contract/sign')
}
