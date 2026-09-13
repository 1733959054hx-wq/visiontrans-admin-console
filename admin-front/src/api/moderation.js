import request from './request'

/**
 * 素材机审判定取值（与后端 MaterialAsset 的 VERDICT_* 常量一一对应）。
 * 组件中禁止再写 '通过' / '人工复审' / '驳回' 裸字符串，统一引用此处常量，避免拼写漂移。
 */
export const ASSET_VERDICTS = { PASS: '通过', REVIEW: '人工复审', REJECT: '驳回' }

/** 素材表单「判定结果」下拉选项（展示顺序）。 */
export const ASSET_VERDICT_OPTIONS = [ASSET_VERDICTS.PASS, ASSET_VERDICTS.REVIEW, ASSET_VERDICTS.REJECT]

/** 术语库审核与 UGC 风控中台大盘。 */
export const fetchModerationOverview = (params) => request.get('/moderation/overview', { params })

/** 素材机审台账轻量列表（广告运营页素材审核区：只取素材行，不加载审核大盘 KPI/看板/UGC）。 */
export const fetchAssets = () => request.get('/moderation/assets')

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
