package com.gzu.adminconsole.jingchen.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.jingchen.entity.MerchantOrderEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantOrderRepository;

/**
 * 商户订单与结算业务逻辑（模块自有）。
 *
 * <p>结算流转:待结算 → 已结算(结算按钮);退款中为终态不可结算。
 */
@Service
public class MerchantOrderService {

    private final MerchantOrderRepository repository;

    public MerchantOrderService(MerchantOrderRepository repository) {
        this.repository = repository;
    }

    /** 订单列表(最新在前)。 */
    public List<MerchantOrderEntity> list() {
        return repository.findAll();
    }

    /** 结算一笔「待结算」订单;退款中与已结算拒绝重复结算。 */
    public MerchantOrderEntity settle(Long id) {
        MerchantOrderEntity order = repository.findById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在: id=" + id);
        }
        if ("退款中".equals(order.getStatus())) {
            throw new BusinessException(400, "退款中的订单不可结算");
        }
        if ("已结算".equals(order.getStatus())) {
            throw new BusinessException(400, "该订单已结算,请勿重复操作");
        }
        order.setStatus("已结算");
        return repository.update(order);
    }
}
