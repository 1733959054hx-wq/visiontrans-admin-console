import request from './request'

/** 术语库审核与 UGC 风控中台大盘。 */
export const fetchModerationOverview = (params) => request.get('/moderation/overview', { params })

/** UGC 违规处置：action = ban / pass / review。 */
export const decideUgc = (action) => request.post('/moderation/ugc/decision', null, { params: { action } })

/** 按平台建议执行退款。 */
export const processRefund = () => request.post('/moderation/refund/process')

/* ------------------------------ 术语包任务 CRUD ------------------------------ */

export const createTask = (payload) => request.post('/moderation/tasks', payload)

export const updateTask = (payload) => request.put('/moderation/tasks', payload)

export const moveTask = (id, direction) => request.patch(`/moderation/tasks/${id}/move`, null, { params: { direction } })

export const deleteTask = (id) => request.delete(`/moderation/tasks/${id}`)

/* ------------------------------ 素材机审 CRUD ------------------------------ */

export const createAsset = (payload) => request.post('/moderation/assets', payload)

export const updateAsset = (payload) => request.put('/moderation/assets', payload)

export const deleteAsset = (id) => request.delete(`/moderation/assets/${id}`)

/** 素材人工复审：decision = pass（通过）/ reject（驳回）。 */
export const reviewAsset = (id, decision) =>
  request.patch(`/moderation/assets/${id}/review`, null, { params: { decision } })

/* ------------------------------ 语种包 / 课程知识包审核 ------------------------------ */

export const createPackage = (payload) => request.post('/moderation/packages', payload)

export const updatePackage = (payload) => request.put('/moderation/packages', payload)

export const deletePackage = (id) => request.delete(`/moderation/packages/${id}`)

/** 知识包审核：decision = pass（通过）/ reject（驳回）。 */
export const reviewPackage = (id, decision) =>
  request.patch(`/moderation/packages/${id}/review`, null, { params: { decision } })
