/**
 * 商户模块（jingchen）。
 *
 * <p>与主工程（{@code com.gzu.adminconsole}）平行的独立业务模块，拥有自己的
 * controller / service / repository / entity / model / dto / common / config 分层，
 * 商户侧的数据结构与业务逻辑一律定义在本模块内，不复用后台管理的 model 与 entity。
 *
 * <p><b>与主工程的关系</b>（以下三点为已确认的共用边界，改动前需双方确认）：
 * <ol>
 *   <li>登录入口共用：商户账号走统一的 {@code /api/auth/login}，令牌与会话由主工程签发；
 *       本模块只负责"商户档案"数据，不自己实现登录。</li>
 *   <li>角色编码 {@value com.gzu.adminconsole.jingchen.common.MerchantConstants#ROLE_CODE}
 *       需在主工程 {@code AuthRepository#roleCodeOf} 中登记映射。</li>
 *   <li>接口鉴权复用主工程 {@code @RequireRole} 注解：本模块接口仅允许商户角色，
 *       后台管理接口仅允许管理角色，实现双向隔离。</li>
 * </ol>
 */
package com.gzu.adminconsole.jingchen;
