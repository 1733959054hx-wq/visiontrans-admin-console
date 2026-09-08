package com.gzu.adminconsole.model;

/**
 * 全球登录监控点位。
 *
 * @param name     城市名
 * @param lon      经度
 * @param lat      纬度
 * @param sessions 会话数
 * @param level    风险等级：ok / warn / bad
 */
public record MonitorPoint(String name, double lon, double lat, int sessions, String level) {
}
