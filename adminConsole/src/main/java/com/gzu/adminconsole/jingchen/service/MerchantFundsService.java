package com.gzu.adminconsole.jingchen.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.jingchen.dto.MerchantFundAmountRequest;
import com.gzu.adminconsole.jingchen.dto.MerchantFundsVO;
import com.gzu.adminconsole.jingchen.entity.MerchantFundFlowEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantWithdrawEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantFundsRepository;

/**
 * 商户资金业务逻辑（模块自有,账户管理-资金账户 / 资金流水 / 提现管理）。
 *
 * <p>余额规则:可用余额 = Σ收入 − Σ支出;充值 / 提现即时记账(演示口径,
 * 真实提现走 T+1 审核流,申请后状态为「审核中」)。所有记账在同事务内完成。
 */
@Service
public class MerchantFundsService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MerchantFundsRepository repository;

    public MerchantFundsService(MerchantFundsRepository repository) {
        this.repository = repository;
    }

    /** 资金总览:余额 / 收支合计 / 流水 / 提现记录。 */
    @Transactional(readOnly = true)
    public MerchantFundsVO overview() {
        List<MerchantFundFlowEntity> flows = repository.findFlows();
        BigDecimal income = flows.stream()
                .filter(f -> "收入".equals(f.getDirection()))
                .map(MerchantFundFlowEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = flows.stream()
                .filter(f -> "支出".equals(f.getDirection()))
                .map(MerchantFundFlowEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = income.subtract(expense);

        List<MerchantFundsVO.FlowItem> flowItems = flows.stream()
                .map(f -> new MerchantFundsVO.FlowItem(f.getId(), f.getFlowType(), f.getDirection(),
                        f.getAmount(), f.getBalanceAfter(), f.getRemark(), f.getCreatedAt()))
                .toList();
        List<MerchantFundsVO.WithdrawItem> withdrawItems = repository.findWithdraws().stream()
                .map(w -> new MerchantFundsVO.WithdrawItem(w.getId(), w.getWithdrawNo(), w.getAmount(),
                        w.getStatus(), w.getAppliedAt(), w.getAccount()))
                .toList();
        return new MerchantFundsVO(balance, income, expense, flowItems, withdrawItems);
    }

    /** 充值:生成收入流水并返回余额快照。 */
    @Transactional
    public MerchantFundFlowEntity recharge(MerchantFundAmountRequest req) {
        BigDecimal amount = requireAmount(req);
        BigDecimal balance = currentBalance().add(amount);
        return repository.saveFlow(new MerchantFundFlowEntity(
                "充值", "收入", amount, balance, "在线充值", now()));
    }

    /** 提现:校验余额充足 → 冻结(生成审核中提现单 + 提现支出流水)。 */
    @Transactional
    public MerchantWithdrawEntity withdraw(MerchantFundAmountRequest req) {
        BigDecimal amount = req.amount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "提现金额必须大于 0");
        }
        if (amount.compareTo(currentBalance()) > 0) {
            throw new BusinessException(400, "提现金额超过可用余额");
        }
        BigDecimal balance = currentBalance().subtract(amount);
        repository.saveFlow(new MerchantFundFlowEntity(
                "提现", "支出", amount, balance, "提现申请冻结", now()));
        return repository.saveWithdraw(new MerchantWithdrawEntity(
                "WD-" + (System.currentTimeMillis() % 1_000_000), amount, "审核中", now(), "工行(****8821)"));
    }

    /** 当前余额 = 全部收入 − 全部支出。 */
    private BigDecimal currentBalance() {
        List<MerchantFundFlowEntity> flows = repository.findFlows();
        BigDecimal balance = BigDecimal.ZERO;
        for (MerchantFundFlowEntity f : flows) {
            balance = "收入".equals(f.getDirection())
                    ? balance.add(f.getAmount())
                    : balance.subtract(f.getAmount());
        }
        return balance;
    }

    private BigDecimal requireAmount(MerchantFundAmountRequest req) {
        if (req == null || req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "充值金额必须大于 0");
        }
        return req.amount();
    }

    private String now() {
        return LocalDateTime.now().format(FMT);
    }
}
