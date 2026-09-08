package com.gzu.adminconsole.jingchen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户 A/B 测试配置（jingchen 模块，业务表 merchant_ab_config,单行配置）。
 *
 * <p>素材管理模块的 A/B 分流:两个对照组素材名 + 对照组 B 的流量占比,
 * 保存后由素材前端按占比展示分流效果。
 */
@Entity
@Table(name = "merchant_ab_config")
public class MerchantAbConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对照 A 素材名。 */
    @Column(name = "material_a", length = 128)
    private String materialA;

    /** 对照 B 素材名。 */
    @Column(name = "material_b", length = 128)
    private String materialB;

    /** B 组流量占比(%,10-90)。 */
    @Column(name = "ratio_b")
    private Integer ratioB;

    protected MerchantAbConfigEntity() {
    }

    public MerchantAbConfigEntity(String materialA, String materialB, Integer ratioB) {
        this.materialA = materialA;
        this.materialB = materialB;
        this.ratioB = ratioB;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMaterialA() { return materialA; }
    public void setMaterialA(String materialA) { this.materialA = materialA; }
    public String getMaterialB() { return materialB; }
    public void setMaterialB(String materialB) { this.materialB = materialB; }
    public Integer getRatioB() { return ratioB; }
    public void setRatioB(Integer ratioB) { this.ratioB = ratioB; }
}
