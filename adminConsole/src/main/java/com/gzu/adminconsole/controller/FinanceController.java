package com.gzu.adminconsole.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.config.RequireRole;
import com.gzu.adminconsole.dto.finance.FinanceOverviewVO;
import com.gzu.adminconsole.dto.meta.ActionResultVO;
import com.gzu.adminconsole.service.FinanceService;

/**
 * 财务订单、商户结算与发票审核接口（View 层）。
 */
@RestController
@ConditionalOnProperty(prefix = "admin-console.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${admin-console.api.base-path:/api}/finance")
@RequireRole({"SUPER_ADMIN", "OPERATIONS", "AUDITOR"})
public class FinanceController {

    private final FinanceService service;

    public FinanceController(FinanceService service) {
        this.service = service;
    }

    /** 财务订单大盘。 */
    @GetMapping("/overview")
    public Result<FinanceOverviewVO> overview() {
        return Result.ok(service.overview());
    }

    /** 处理 C 端订单：action = markPaid（确认支付）/ refund（退款）/ close（关闭）（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PostMapping("/orders/{id}/handle")
    public Result<ActionResultVO> handleOrder(@PathVariable Long id,
                                              @RequestParam String action) {
        return Result.ok(service.handleOrder(id, action));
    }

    /** 商户结算对账：待对账 → 已对账（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PostMapping("/settlements/{id}/reconcile")
    public Result<ActionResultVO> reconcile(@PathVariable Long id) {
        return Result.ok(service.reconcile(id));
    }

    /** 商户结算打款：已对账 → 已结算（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PostMapping("/settlements/{id}/settle")
    public Result<ActionResultVO> settle(@PathVariable Long id) {
        return Result.ok(service.settle(id));
    }

    /** 发票审核：approved = true 开票 / false 驳回（运营管理员及以上）。 */
    @RequireRole({"SUPER_ADMIN", "OPERATIONS"})
    @PostMapping("/invoices/{id}/review")
    public Result<ActionResultVO> reviewInvoice(@PathVariable Long id,
                                                @RequestParam boolean approved) {
        return Result.ok(service.reviewInvoice(id, approved));
    }
}
