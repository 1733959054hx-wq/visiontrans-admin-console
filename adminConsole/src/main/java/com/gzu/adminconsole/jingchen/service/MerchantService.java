package com.gzu.adminconsole.jingchen.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.config.JingchenProperties;
import com.gzu.adminconsole.jingchen.dto.MerchantHomeVO;
import com.gzu.adminconsole.jingchen.entity.MerchantEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantRepository;

/**
 * 商户工作台业务逻辑（模块自有）。
 *
 * <p>数据一律取自本模块的 merchant_account，不查询后台管理的任何表。
 */
@Service
public class MerchantService {

    private final MerchantRepository repository;
    private final JingchenProperties properties;

    public MerchantService(MerchantRepository repository, JingchenProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * 组装工作台首页数据。
     *
     * @param loginName  当前登录账号（商户编码）
     * @param displayName 会话中的显示名，档案缺失时兜底
     * @param roleName   会话中的角色中文名
     */
    public MerchantHomeVO home(String loginName, String displayName, String roleName) {
        MerchantEntity entity = repository.findByCode(loginName);
        String name = entity != null && entity.getName() != null ? entity.getName() : displayName;
        String contact = entity != null ? entity.getContact() : null;
        return new MerchantHomeVO(
                properties.getWorkspaceTitle(),
                name,
                loginName,
                contact,
                roleName == null || roleName.isBlank() ? MerchantConstants.ROLE_NAME : roleName,
                "欢迎回来，" + name + "！这里是您的专属工作台。",
                List.of(
                        "本页为商户角色独立入口，后台管理的页面与数据对您不可见。",
                        "页面模块由前端组员填充，业务接口由后端组员补充。"));
    }
}
