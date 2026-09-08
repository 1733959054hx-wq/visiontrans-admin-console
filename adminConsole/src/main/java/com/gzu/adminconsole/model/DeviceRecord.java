package com.gzu.adminconsole.model;

/**
 * 设备登录审计记录。
 *
 * @param region      地域 · 运营商
 * @param ip          IP 地址
 * @param fingerprint 设备指纹，业务唯一键
 * @param sessions    会话数文案
 * @param risk        风险分（0-100）
 * @param verdict     判定：正常 / 可疑 / 异常
 * @param banned      是否已封禁
 */
public record DeviceRecord(String region,
                           String ip,
                           String fingerprint,
                           String sessions,
                           int risk,
                           String verdict,
                           boolean banned) {

    /** 复制一份并替换为已封禁状态（避免与记录访问器 banned() 重名）。 */
    public DeviceRecord markBanned() {
        return new DeviceRecord(region, ip, fingerprint, sessions, risk, "已封禁", true);
    }
}
