package com.gzu.adminconsole.dto.security;

/**
 * RBAC 权限三态更新请求体。
 *
 * @param roleCode       角色编码
 * @param groupName      权限组名称
 * @param permissionName 权限名称
 * @param state          目标状态：GRANTED / PARTIAL / NONE
 */
public record PermissionUpdateRequest(String roleCode, String groupName, String permissionName, String state) {
}
