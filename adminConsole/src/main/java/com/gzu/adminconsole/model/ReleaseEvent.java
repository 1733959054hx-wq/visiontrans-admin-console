package com.gzu.adminconsole.model;

/**
 * 模型版本迭代与运维时间线事件。
 *
 * @param title 事件标题
 * @param time  发生时间
 * @param desc  事件描述
 * @param tone  展示色调：green / amber / rose
 */
public record ReleaseEvent(String title, String time, String desc, String tone) {
}
