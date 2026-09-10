package com.gzu.adminconsole.jingchen.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.jingchen.dto.MerchantGoodsRequest;
import com.gzu.adminconsole.jingchen.entity.MerchantGoodsEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantGoodsRepository;

/**
 * 商户商品管理业务逻辑（模块自有）。
 *
 * <p>定价策略:折后价 = 原价 × 折扣% 后端统一计算,前端不可直填折后价;
 * 上架 / 下架只允许在「在售 / 已下架」两态间流转。
 */
@Service
public class MerchantGoodsService {

    private final MerchantGoodsRepository repository;

    public MerchantGoodsService(MerchantGoodsRepository repository) {
        this.repository = repository;
    }

    public List<MerchantGoodsEntity> list() {
        return repository.findAll();
    }

    public MerchantGoodsEntity get(Long id) {
        MerchantGoodsEntity goods = repository.findById(id);
        if (goods == null) {
            throw new BusinessException(404, "商品不存在: id=" + id);
        }
        return goods;
    }

    /** 新建上架:按定价策略计算折后价。 */
    public MerchantGoodsEntity create(MerchantGoodsRequest req) {
        int discount = normalizeDiscount(req.discount());
        MerchantGoodsEntity goods = new MerchantGoodsEntity(
                requireName(req.goodsName()),
                req.goodsType() == null ? "专业词典" : req.goodsType(),
                req.description(),
                req.price() == null ? BigDecimal.ZERO : req.price(),
                discount,
                calcSale(req.price(), discount),
                "在售",
                req.salesCount() == null ? 0L : req.salesCount());
        return repository.save(goods);
    }

    /** 编辑:重算折后价;销量为系统字段不接受修改。 */
    public MerchantGoodsEntity update(Long id, MerchantGoodsRequest req) {
        MerchantGoodsEntity goods = get(id);
        if (req.goodsName() != null && !req.goodsName().isBlank()) goods.setGoodsName(req.goodsName().trim());
        if (req.goodsType() != null) goods.setGoodsType(req.goodsType());
        if (req.description() != null) goods.setDescription(req.description());
        if (req.price() != null) goods.setPrice(req.price());
        if (req.discount() != null) {
            goods.setDiscount(normalizeDiscount(req.discount()));
            goods.setSalePrice(calcSale(goods.getPrice(), goods.getDiscount()));
        }
        if (req.price() != null) {
            goods.setPrice(req.price());
            goods.setSalePrice(calcSale(req.price(), goods.getDiscount()));
        }
        return repository.save(goods);
    }

    /** 上架 / 下架。 */
    public MerchantGoodsEntity toggleShelf(Long id, boolean shelf) {
        MerchantGoodsEntity goods = get(id);
        goods.setStatus(shelf ? "在售" : "已下架");
        return repository.save(goods);
    }

    public void delete(Long id) {
        repository.delete(get(id));
    }

    private int normalizeDiscount(Integer discount) {
        if (discount == null) {
            return 100;
        }
        return Math.min(100, Math.max(30, discount));
    }

    private BigDecimal calcSale(BigDecimal price, Integer discount) {
        BigDecimal base = price == null ? BigDecimal.ZERO : price;
        return base.multiply(BigDecimal.valueOf(discount)).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "商品名称必填");
        }
        return name.trim();
    }
}
