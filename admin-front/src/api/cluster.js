import request from './request'

/** 集群态势感知与推演监控大盘。 */
export const fetchClusterOverview = (params) => request.get('/cluster/overview', { params })

/** 容量推演。 */
export const simulateCapacity = () => request.post('/cluster/capacity-simulation')

/* ------------------------------ 容器节点 CRUD ------------------------------ */

export const createNode = (payload) => request.post('/cluster/nodes', payload)

export const updateNode = (payload) => request.put('/cluster/nodes', payload)

export const deleteNode = (id) => request.delete(`/cluster/nodes/${encodeURIComponent(id)}`)

/* ------------------------------ 告警事件 CRUD ------------------------------ */

export const createAlarm = (payload) => request.post('/cluster/alarms', payload)

export const deleteAlarm = (id) => request.delete(`/cluster/alarms/${id}`)

/* ------------------------------ 运维管理（熔断 / 备份 / 阈值） ------------------------------ */

/** 运维管理数据：熔断降级策略、备份策略与资源告警阈值。 */
export const fetchClusterOps = () => request.get('/cluster/ops')

/** 系统日志检索：level（INFO/WARN/ERROR）与 category 均可空。 */
export const fetchClusterSysLogs = (params) => request.get('/cluster/syslogs', { params })

/** 更新熔断器状态与启用（body {id, state, enabled}）。 */
export const updateBreaker = (payload) => request.put('/cluster/ops/breakers', payload)

/** 更新备份策略启用（body {id, enabled}）。 */
export const updateBackup = (payload) => request.put('/cluster/ops/backups', payload)

/** 更新资源告警阈值（cpu / mem / gpu）。 */
export const updateThresholds = (cpu, mem, gpu) =>
  request.put('/cluster/ops/thresholds', null, { params: { cpu, mem, gpu } })

/* ---------------------- 核心服务与第三方接口可用性 ---------------------- */

/** 依赖服务可用性列表（响应耗时 / 可用率 / 最近拨测时间）。 */
export const fetchDependencies = () => request.get('/cluster/dependencies')

/** 对单个依赖服务发起一次真实拨测。 */
export const probeDependency = (id) => request.post(`/cluster/dependencies/${id}/probe`)

/** 一键拨测全部依赖服务。 */
export const probeAllDependencies = () => request.post('/cluster/dependencies/probe-all')
