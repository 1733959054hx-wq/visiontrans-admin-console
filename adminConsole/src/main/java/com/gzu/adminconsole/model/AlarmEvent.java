package com.gzu.adminconsole.model;

/**
 * 集群告警与自愈事件。
 *
 * @param id      主键，新增时为 null
 * @param time    触发时间
 * @param level   告警等级：P1 / P2 / P3
 * @param message 告警内容
 * @param result  处置结果
 */
public record AlarmEvent(Long id, String time, String level, String message, String result) {

    /** 构造一条新告警（无主键）。 */
    public static AlarmEvent of(String time, String level, String message, String result) {
        return new AlarmEvent(null, time, level, message, result);
    }
}
