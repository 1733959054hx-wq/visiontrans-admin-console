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
