import request from './request'

/** 广告位排期与调度引擎大盘。 */
export const fetchAdOverview = (params) => request.get('/ads/overview', { params })

/** 更新单用户频次限制。 */
export const updateFrequency = (name, value) => request.put('/ads/frequency', { name, value })

/** 采纳 AI 智能调优建议。 */
export const adoptAdvice = () => request.post('/ads/advice/adopt')

/* ------------------------------ 广告位 CRUD ------------------------------ */

export const createSlot = (payload) => request.post('/ads/slots', payload)

export const updateSlot = (payload) => request.put('/ads/slots', payload)

export const deleteSlot = (id) => request.delete(`/ads/slots/${id}`)
