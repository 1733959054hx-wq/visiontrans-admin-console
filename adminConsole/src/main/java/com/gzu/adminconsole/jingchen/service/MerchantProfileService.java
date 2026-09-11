package com.gzu.adminconsole.jingchen.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.jingchen.dto.MerchantProfileRequest;
import com.gzu.adminconsole.jingchen.dto.MerchantProfileVO;
import com.gzu.adminconsole.jingchen.entity.MerchantEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantProfileExtEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantProfileRepository;

/**
 * 商户资料维护业务逻辑（jingchen 模块,账户管理-基本信息维护）。
 *
 * <p>展示名与商户编码由会话决定不可修改;可维护联系人 / 电话 / 结算账户。
 */
@Service
public class MerchantProfileService {

    private final MerchantProfileRepository repository;

    public MerchantProfileService(MerchantProfileRepository repository) {
        this.repository = repository;
    }

    /** 查看当前商户资料。 */
    @Transactional(readOnly = true)
    public MerchantProfileVO get(String code) {
        MerchantEntity account = requireAccount(code);
        MerchantProfileExtEntity ext = repository.findProfileExt(code);
        return new MerchantProfileVO(
                account.getCode(),
                account.getName(),
                account.getContact(),
                account.getPhone(),
                ext == null ? null : ext.getSettleAccount(),
                account.getStatus(),
                account.getLastLogin());
    }

    /** 维护资料:仅联系人 / 电话 / 结算账户可改。 */
    @Transactional
    public MerchantProfileVO update(String code, MerchantProfileRequest req) {
        MerchantEntity account = requireAccount(code);
        if (req.contact() != null) account.setContact(req.contact().trim());
        if (req.phone() != null) account.setPhone(req.phone().trim());
        repository.saveAccount(account);

        String settle = req.settleAccount();
        if (settle != null) {
            MerchantProfileExtEntity ext = repository.findProfileExt(code);
            if (ext == null) {
                ext = new MerchantProfileExtEntity(code, settle.trim());
            } else {
                ext.setSettleAccount(settle.trim());
            }
            repository.saveProfileExt(ext);
        }
        return get(code);
    }

    private MerchantEntity requireAccount(String code) {
        MerchantEntity account = repository.findAccount(code);
        if (account == null) {
            throw new BusinessException(404, "商户不存在: " + code);
        }
        return account;
    }
}
