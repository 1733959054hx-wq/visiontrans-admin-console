package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户入驻申请（资质提交 / 合同签署的后台审核对象）。
 */
@Entity
@Table(name = "merchant_onboarding")
public class MerchantOnboardingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 申请单号。 */
    @Column(name = "apply_no", length = 32)
    private String applyNo;

    /** 商户名称。 */
    @Column(name = "merchant_name", length = 128)
    private String merchantName;

    /** 统一社会信用代码 / 营业执照号。 */
    @Column(name = "license_no", length = 64)
    private String licenseNo;

    /** 联系人。 */
    @Column(name = "contact", length = 64)
    private String contact;

    /** 联系电话。 */
    @Column(name = "phone", length = 32)
    private String phone;

    /** 资质文件：营业执照等。 */
    @Column(name = "qualification", length = 128)
    private String qualification;

    /** 合同状态：待签署 / 已签署。 */
    @Column(name = "contract_status", length = 16)
    private String contractStatus;

    /** 审核状态：待审核 / 已通过 / 已驳回。 */
    @Column(name = "audit_status", length = 16)
    private String status;

    /** 提交时间。 */
    @Column(name = "submitted", length = 32)
    private String submitted;

    /** 审核人。 */
    @Column(name = "reviewer", length = 32)
    private String reviewer;

    /** 审核意见。 */
    @Column(name = "remark", length = 256)
    private String remark;

    protected MerchantOnboardingEntity() {
    }

    public MerchantOnboardingEntity(String applyNo, String merchantName, String licenseNo, String contact,
                                    String phone, String qualification, String contractStatus, String status,
                                    String submitted, String reviewer, String remark) {
        this.applyNo = applyNo;
        this.merchantName = merchantName;
        this.licenseNo = licenseNo;
        this.contact = contact;
        this.phone = phone;
        this.qualification = qualification;
        this.contractStatus = contractStatus;
        this.status = status;
        this.submitted = submitted;
        this.reviewer = reviewer;
        this.remark = remark;
    }

    public Long getId() { return id; }
    public String getApplyNo() { return applyNo; }
    public String getMerchantName() { return merchantName; }
    public String getLicenseNo() { return licenseNo; }
    public String getContact() { return contact; }
    public String getPhone() { return phone; }
    public String getQualification() { return qualification; }
    public String getContractStatus() { return contractStatus; }
    public String getStatus() { return status; }
    public String getSubmitted() { return submitted; }
    public String getReviewer() { return reviewer; }
    public String getRemark() { return remark; }

    public void setContractStatus(String contractStatus) { this.contractStatus = contractStatus; }
    public void setStatus(String status) { this.status = status; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    public void setRemark(String remark) { this.remark = remark; }
}
