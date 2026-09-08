import request from './request'

/** 模型热更中心大盘。 */
export const fetchModelOverview = (params) => request.get('/models/overview', { params })

/** 调整全局灰度比例。 */
export const updateGrayscale = (ratio) => request.put('/models/grayscale', null, { params: { ratio } })

/** 指定模型秒级热更到全量。 */
export const hotUpdateModel = (name) => request.post('/models/hot-update', null, { params: { name } })

/** 指定模型一键回滚。 */
export const rollbackModel = (name) => request.post('/models/rollback', null, { params: { name } })

/** 切换灰度 / 热更策略开关。 */
export const updateStrategy = (name, enabled) =>
  request.put('/models/strategies', null, { params: { name, enabled } })

/* ------------------------------ 模型 CRUD ------------------------------ */

export const createModel = (payload) => request.post('/models', payload)

export const updateModel = (payload) => request.put('/models', payload)

export const deleteModel = (name) => request.delete('/models', { params: { name } })
