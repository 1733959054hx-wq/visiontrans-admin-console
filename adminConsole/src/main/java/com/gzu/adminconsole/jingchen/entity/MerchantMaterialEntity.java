package com.gzu.adminconsole.jingchen.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户广告素材实体（jingchen 模块，业务表 merchant_material）。
 *
 * <p>素材管理模块:图片 / 视频 / H5 三种形态的投放素材档案,
 * 含投放表现数据与 A/B 分组标记;演示数据由 {@code MerchantMaterialDataInitializer} 灌入。
 */
@Entity
@Table(name = "merchant_material")
public class MerchantMaterialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 素材文件名,如 东京导览_15s.mp4。 */
    @Column(name = "material_name", length = 128)
    private String name;

    /** 素材形态:图片 / 视频 / H5。 */
    @Column(name = "material_type", length = 16)
    private String materialType;

    /** 文件大小(KB)。 */
    @Column(name = "size_kb")
    private Long sizeKb;

    /** 曝光量(次)。 */
    @Column(name = "exposure")
    private Long exposure;

    /** 点击率(%),可为空。 */
    @Column(name = "ctr", precision = 5, scale = 2)
    private BigDecimal ctr;

    /** 状态:使用中 / 测试中 / 已停用。 */
    @Column(name = "m_status", length = 16)
    private String status;

    protected MerchantMaterialEntity() {
    }

    public MerchantMaterialEntity(String name, String materialType, Long sizeKb, Long exposure,
                                  BigDecimal ctr, String status) {
        this.name = name;
        this.materialType = materialType;
        this.sizeKb = sizeKb;
        this.exposure = exposure;
        this.ctr = ctr;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public Long getSizeKb() { return sizeKb; }
    public void setSizeKb(Long sizeKb) { this.sizeKb = sizeKb; }
    public Long getExposure() { return exposure; }
    public void setExposure(Long exposure) { this.exposure = exposure; }
    public BigDecimal getCtr() { return ctr; }
    public void setCtr(BigDecimal ctr) { this.ctr = ctr; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
