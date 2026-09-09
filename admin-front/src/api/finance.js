import request from './request'

/** 财务订单与商户结算中心大盘。 */
export const fetchFinanceOverview = (params) => request.get('/finance/overview', { params })

/** 订单处置：action = markPaid（标记已付）/ refund（退款）/ close（关闭）。 */
export const handleOrder = (id, action) =>
  request.post(`/finance/orders/${id}/handle`, null, { params: { action } })

/** 商户账期对账。 */
export const reconcileSettlement = (id) => request.post(`/finance/settlements/${id}/reconcile`)

/** 商户分账结算。 */
export const settleSettlement = (id) => request.post(`/finance/settlements/${id}/settle`)

/** B 端发票审核：approved = true 通过 / false 驳回。 */
export const reviewInvoice = (id, approved) =>
  request.post(`/finance/invoices/${id}/review`, null, { params: { approved } })

/* ------------------------------ 商户入驻审核 ------------------------------ */

/** 入驻审核：approved = true 通过 / false 驳回。 */
export const reviewOnboarding = (id, approved) =>
  request.post(`/finance/onboardings/${id}/review`, null, { params: { approved } })

/** 标记平台合作协议已签署。 */
export const signOnboarding = (id) => request.post(`/finance/onboardings/${id}/sign`)
