import request from './request'

/** 安全风控大盘（page 为审计日志页码）。 */
export const fetchSecurityOverview = (page = 1, range = {}) =>
  request.get('/security/overview', { params: { page, ...range } })

/** 封禁异常设备。 */
export const banDevice = (fingerprint) => request.post('/security/devices/ban', null, { params: { fingerprint } })

/** 移除指定设备的登录态（踢下线），保留设备台账。 */
export const kickDevice = (fingerprint) =>
  request.post(`/security/devices/${encodeURIComponent(fingerprint)}/kick`)

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

/* ------------------------------ C 端用户 ------------------------------ */

/** C 端用户列表（服务端条件筛选 + 分页）。 */
export const fetchAppUsers = (params) => request.get('/security/app-users', { params })

/** 更新 C 端用户（会员状态 / 账号状态，body 同 appUser 对象含 id）。 */
export const updateUser = (payload) => request.put('/security/app-users', payload)

/* ---------------------------- 菜单权限配置 ---------------------------- */

/** 菜单 × 角色可见性矩阵。 */
export const fetchMenuPermissions = () => request.get('/security/menu-permissions')

/** 切换某角色对某菜单的可见性。 */
export const updateMenuPermission = (roleCode, menuId, visible) =>
  request.put('/security/menu-permissions', null, { params: { roleCode, menuId, visible } })

/* ------------------------------ 系统配置 ------------------------------ */

/** 系统配置：运行参数 + 功能开关。 */
export const fetchSysConfig = () => request.get('/security/sysconfig')

/** 更新系统参数（value 为数字字符串）。 */
export const updateSysParam = (name, value) =>
  request.put('/security/sysparams', null, { params: { name, value } })

/** 切换系统功能开关。 */
export const updateFeature = (name, enabled) =>
  request.put('/security/sysfeatures', null, { params: { name, enabled } })
