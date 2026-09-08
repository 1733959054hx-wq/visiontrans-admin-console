import request from './request'

/** 侧边导航菜单。 */
export const fetchNav = () => request.get('/meta/nav')

/** 系统展示信息（品牌、集群状态、当前管理员）。 */
export const fetchSystem = () => request.get('/meta/system')
