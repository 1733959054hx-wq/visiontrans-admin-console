package com.gzu.adminconsole.model;

/**
 * C 端用户账号。
 *
 * @param id         主键，新增时为 null
 * @param account    账号昵称
 * @param regSource  注册来源：手机号 / 微信 / QQ / Apple
 * @param membership 会员状态：免费体验 / 会员月卡 / 会员年卡 / 会员过期
 * @param registered 注册时间
 * @param lastActive 最近活跃时间
 * @param status     状态：正常 / 停用
 */
public record AppUser(Long id,
                      String account,
                      String regSource,
                      String membership,
                      String registered,
                      String lastActive,
                      String status) {
}
