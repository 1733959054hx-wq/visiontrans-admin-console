package com.gzu.adminconsole.jingchen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 商户模块自有配置（{@code admin-console.jingchen.*}）。
 *
 * <p>与主工程配置分开，方便本模块独立调整而不影响后台管理。
 */
@Component
@ConfigurationProperties(prefix = "admin-console.jingchen")
public class JingchenProperties {

    /** 是否启用商户模块（关闭后接口不注册、不初始化账号）。 */
    private boolean enabled = true;

    /** 演示商户登录账号。 */
    private String username = "merchant";

    /** 演示商户登录口令（演示用途，启动时重置）。 */
    private String password = "merchant123";

    /** 工作台标题。 */
    private String workspaceTitle = "商户工作台";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getWorkspaceTitle() {
        return workspaceTitle;
    }

    public void setWorkspaceTitle(String workspaceTitle) {
        this.workspaceTitle = workspaceTitle;
    }
}
