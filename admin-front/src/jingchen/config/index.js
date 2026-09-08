/**
 * 商户模块（jingchen）配置常量。
 *
 * 与主工程 src/ 平级：本模块拥有自己的 config / api / stores / components / views / router，
 * 只通过这里定义的常量与主工程交互，避免硬编码散落各处。
 */

/** 角色编码（与后端 MerchantConstants#ROLE_CODE 一致） */
export const MERCHANT_ROLE = 'MERCHANT'

/** 后台管理角色编码（与后端 roleCodeOf 映射一致） */
export const ADMIN_ROLES = ['SUPER_ADMIN', 'OPERATIONS', 'AUDITOR']

/** 商户模块路由前缀 */
export const MERCHANT_ROUTE_PREFIX = '/merchant'

/** 模块展示名 */
export const MERCHANT_MODULE_NAME = '商户工作台'
