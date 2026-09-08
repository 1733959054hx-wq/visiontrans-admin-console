import request from './request'

/** 安全风控大盘（page 为审计日志页码）。 */
export const fetchSecurityOverview = (page = 1, range = {}) =>
  request.get('/security/overview', { params: { page, ...range } })

/** 封禁异常设备。 */
export const banDevice = (fingerprint) => request.post('/security/devices/ban', null, { params: { fingerprint } })

/** 更新 RBAC 三态授权。 */
export const updatePermission = (payload) => request.put('/security/permissions', payload)

/** 切换安全策略开关。 */
export const updatePolicy = (name, enabled) =>
  request.put('/security/policies', null, { params: { name, enabled } })

/* ------------------------------ 设备 CRUD ------------------------------ */

export const createDevice = (payload) => request.post('/security/devices', payload)

export const updateDevice = (payload) => request.put('/security/devices', payload)

export const deleteDevice = (fingerprint) => request.delete(`/security/devices/${encodeURIComponent(fingerprint)}`)

/* ------------------------------ 套餐 CRUD ------------------------------ */

export const createPlan = (payload) => request.post('/security/plans', payload)

export const updatePlan = (payload) => request.put('/security/plans', payload)

export const deletePlan = (name) => request.delete(`/security/plans/${encodeURIComponent(name)}`)

/* ------------------------------ 管理员 CRUD ------------------------------ */

export const createAdmin = (payload) => request.post('/security/admins', payload)

export const updateAdmin = (payload) => request.put('/security/admins', payload)

export const deleteAdmin = (id) => request.delete(`/security/admins/${id}`)
