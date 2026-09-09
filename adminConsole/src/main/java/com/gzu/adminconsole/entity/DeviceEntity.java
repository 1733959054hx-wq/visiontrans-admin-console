package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 设备登录审计记录。
 */
@Entity
@Table(name = "device_record")
public class DeviceEntity {

    @Id
    @Column(name = "fingerprint", length = 64)
    private String fingerprint;

    @Column(name = "region", length = 64)
    private String region;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "sessions", length = 32)
    private String sessions;

    @Column(name = "risk")
    private int risk;

    @Column(name = "verdict", length = 16)
    private String verdict;

    @Column(name = "banned")
    private boolean banned;

    /** 最近一次登录的 C 端账号：用于按设备移除登录态（踢下线）。 */
    @Column(name = "account", length = 64)
    private String account;

    @Column(name = "sort_order")
    private int sortOrder;

    protected DeviceEntity() {
    }

    public DeviceEntity(String fingerprint, String region, String ip, String sessions, int risk,
                        String verdict, boolean banned, String account, int sortOrder) {
        this.fingerprint = fingerprint;
        this.region = region;
        this.ip = ip;
        this.sessions = sessions;
        this.risk = risk;
        this.verdict = verdict;
        this.banned = banned;
        this.account = account;
        this.sortOrder = sortOrder;
    }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getSessions() { return sessions; }
    public void setSessions(String sessions) { this.sessions = sessions; }
    public int getRisk() { return risk; }
    public void setRisk(int risk) { this.risk = risk; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
