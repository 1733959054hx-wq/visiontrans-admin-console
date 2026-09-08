package com.gzu.adminconsole;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 视界译 VisionTrans · 平台管理后台启动类。
 *
 * <p>架构分层（MVVM 在前后端的落地方式）：
 * <ul>
 *   <li>Model —— {@code model} 领域实体 + {@code repository} 数据访问（当前为内存实现，可平滑替换为 JPA / MyBatis）</li>
 *   <li>ViewModel —— {@code service} 业务编排 + {@code dto} 视图模型（为前端页面量身聚合的数据）</li>
 *   <li>View —— {@code controller} REST 接口（前端 Vue 组件通过 axios 消费）</li>
 * </ul>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AdminConsoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminConsoleApplication.class, args);
    }
}
