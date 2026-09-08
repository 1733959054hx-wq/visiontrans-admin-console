package com.gzu.adminconsole.common;

/**
 * 令牌解析器 SPI。
 *
 * <p>主工程（{@code AuthRepository}）与各业务模块各提供一个实现，
 * {@code AuthInterceptor} 按顺序依次尝试，第一个命中的实现负责把身份写入
 * {@link com.gzu.adminconsole.config.AdminContext}。
 *
 * <p>这样新增模块（如 jingchen 商户模块）可以拥有自己的会话表与令牌，
 * 而无需改动主工程的鉴权主流程 —— 符合开闭原则。
 */
public interface TokenResolver {

    /**
     * 按令牌解析会话并在命中时绑定当前身份。
     *
     * @param token 请求头中的令牌，可能为 null / 空
     * @return true = 命中并已写入上下文；false = 本解析器不认识该令牌，交给下一个
     */
    boolean resolveAndBind(String token);
}
