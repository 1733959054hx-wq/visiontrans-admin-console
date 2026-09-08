package com.gzu.adminconsole.model;

/**
 * 细粒度权限项（三态授权）。
 *
 * @param name  权限名称
 * @param state 授权状态：GRANTED / PARTIAL / NONE
 */
public record Permission(String name, String state) {

    /** 已授权 */
    public static final String GRANTED = "GRANTED";
    /** 部分授权 */
    public static final String PARTIAL = "PARTIAL";
    /** 未授权 */
    public static final String NONE = "NONE";

    /** 复制一份并替换授权状态。 */
    public Permission withState(String newState) {
        return new Permission(name, newState);
    }
}
