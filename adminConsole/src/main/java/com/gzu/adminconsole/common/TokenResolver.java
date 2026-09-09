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

    /**
     * 该请求路径是否属于本实现负责的「免鉴权公开路径」（登录、验证码、公钥下发 …）。
     *
     * <p>主干 {@code AuthInterceptor} 依次询问各实现，命中即放行；默认不公开。
     * 由此「谁提供接口、谁声明放行」，主干不再写死任何业务模块的 URL。
     *
     * @param uri 当前请求的 URI（可能为 null）
     * @return true = 无需令牌即可访问
     */
    default boolean isPublicPath(String uri) {
        return false;
    }
}
