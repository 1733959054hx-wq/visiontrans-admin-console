package com.gzu.adminconsole.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.gzu.adminconsole.config.AuthInterceptor;

/**
 * Web 层配置：跨域策略与登录鉴权拦截器。
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties properties;

    @Autowired
    private AuthInterceptor authInterceptor;

    public WebConfig(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 鉴权规则由各接口上的 @RequireRole 决定，登录接口无需标注因而天然放行
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        AppProperties.Cors cors = properties.getCors();
        if (!cors.isEnabled()) {
            return;
        }
        registry.addMapping("/**")
                .allowedOriginPatterns(cors.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods(cors.getAllowedMethods().split(","))
                .allowedHeaders(cors.getAllowedHeaders())
                .allowCredentials(cors.isAllowCredentials())
                .maxAge(cors.getMaxAge());
    }
}
