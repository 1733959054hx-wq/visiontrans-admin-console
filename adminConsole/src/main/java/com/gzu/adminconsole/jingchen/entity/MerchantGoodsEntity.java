package com.gzu.adminconsole.jingchen.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户商品实体（jingchen 模块，业务表 merchant_goods）。
 *
 * <p>内容分销-商品管理:上架 / 编辑 / 下架与折扣定价的数据底座;
 * 订单表(merchant_order)的 goodsName 与本表关联。
 */
@Entity
@Table(name = "merchant_goods")
public class MerchantGoodsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 商品名称。 */
    @Column(name = "goods_name", length = 128)
    private String goodsName;

    /** 商品类型:专业词典 / 视频课程 / 文化包 / 视频内容。 */
    @Column(name = "goods_type", length = 16)
    private String goodsType;

    /** 商品描述。 */
    @Column(name = "goods_desc", length = 255)
    private String description;

    /** 原价(元)。 */
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    /** 折扣(%,30-100,100=不打折)。 */
    @Column(name = "discount")
    private Integer discount;

    /** 折后售价(元,保存时按 price × discount 计算)。 */
    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    /** 上架状态:在售 / 已下架。 */
    @Column(name = "goods_status", length = 16)
    private String status;

    /** 累计销量(份)。 */
    @Column(name = "sales_count")
    private Long salesCount;

    protected MerchantGoodsEntity() {
    }

    public MerchantGoodsEntity(String goodsName, String goodsType, String description,
                               BigDecimal price, Integer discount, BigDecimal salePrice,
                               String status, Long salesCount) {
        this.goodsName = goodsName;
        this.goodsType = goodsType;
        this.description = description;
        this.price = price;
        this.discount = discount;
        this.salePrice = salePrice;
        this.status = status;
        this.salesCount = salesCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public String getGoodsType() { return goodsType; }
    public void setGoodsType(String goodsType) { this.goodsType = goodsType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getDiscount() { return discount; }
    public void setDiscount(Integer discount) { this.discount = discount; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getSalesCount() { return salesCount; }
    public void setSalesCount(Long salesCount) { this.salesCount = salesCount; }
}
