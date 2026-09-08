package com.gzu.adminconsole.dto.meta;

/**
 * 系统级展示信息视图模型（品牌、集群状态、当前登录管理员）。
 */
public record SystemStatusVO(String name,
                             String subtitle,
                             String consoleLabel,
                             String groupName,
                             String clusterStatus,
                             String clusterLatency,
                             String dateRange,
                             String currentUser,
                             String currentUserId,
                             String currentUserAvatar) {
}
