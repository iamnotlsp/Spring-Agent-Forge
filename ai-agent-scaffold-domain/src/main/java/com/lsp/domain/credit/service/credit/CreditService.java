package com.lsp.domain.credit.service.credit;

import com.lsp.domain.credit.adapter.repository.ICreditRepository;
import com.lsp.domain.credit.model.entity.CreditAccountEntity;
import com.lsp.domain.credit.model.entity.CreditGrantEntity;
import com.lsp.domain.credit.model.entity.CreditGrantResultEntity;
import com.lsp.domain.credit.model.entity.CreditOrderEntity;
import com.lsp.domain.credit.model.entity.CreditUseEntity;
import com.lsp.domain.credit.model.entity.GroupBuyTeamSuccessEntity;
import com.lsp.domain.credit.model.valobj.enums.CreditGrantTypeEnumVO;
import com.lsp.domain.credit.model.valobj.enums.CreditOrderStatusEnumVO;
import com.lsp.domain.credit.service.ICreditService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 额度领域服务实现。
 */
@Service
public class CreditService implements ICreditService {

    private final ICreditRepository creditRepository;

    public CreditService(ICreditRepository creditRepository) {
        this.creditRepository = creditRepository;
    }

    @Override
    public void createCreditOrder(CreditOrderEntity creditOrderEntity) {
        checkCreditOrder(creditOrderEntity);

        CreditOrderEntity existedCreditOrder = creditRepository.queryCreditOrderByOutTradeNo(creditOrderEntity.getOutTradeNo().trim());
        if (null != existedCreditOrder) {
            checkSameCreditOrder(existedCreditOrder, creditOrderEntity);
            syncCreditOrderAfterPayIfNecessary(existedCreditOrder, creditOrderEntity);
            return;
        }

        if (!isBlank(creditOrderEntity.getTeamId())) {
            creditOrderEntity.setStatus(CreditOrderStatusEnumVO.WAIT_GROUP);
        } else if (null == creditOrderEntity.getStatus()) {
            creditOrderEntity.setStatus(CreditOrderStatusEnumVO.WAIT_GROUP);
        }
        if (null == creditOrderEntity.getPaidTime()) {
            creditOrderEntity.setPaidTime(LocalDateTime.now());
        }

        creditRepository.saveCreditOrder(creditOrderEntity);
    }

    @Override
    public CreditOrderEntity purchaseCreditOrder(CreditOrderEntity creditOrderEntity) {
        checkCreditOrder(creditOrderEntity);
        creditOrderEntity.setTeamId(null);

        CreditOrderEntity existedCreditOrder = creditRepository.queryCreditOrderByOutTradeNo(creditOrderEntity.getOutTradeNo());
        if (null != existedCreditOrder) {
            checkSameCreditOrder(existedCreditOrder, creditOrderEntity);
            if (!isBlank(existedCreditOrder.getTeamId())) {
                throw new IllegalArgumentException("outTradeNo already exists with group buy order");
            }
            if (CreditOrderStatusEnumVO.CREDIT_GRANTED == existedCreditOrder.getStatus()
                    || creditRepository.isCreditGranted(existedCreditOrder.getOutTradeNo())) {
                return creditRepository.queryCreditOrderByOutTradeNo(existedCreditOrder.getOutTradeNo());
            }

            grantDirectPurchaseCredits(existedCreditOrder);
            return creditRepository.queryCreditOrderByOutTradeNo(existedCreditOrder.getOutTradeNo());
        }

        LocalDateTime now = LocalDateTime.now();
        creditOrderEntity.setStatus(CreditOrderStatusEnumVO.WAIT_GROUP);
        creditOrderEntity.setPaidTime(null == creditOrderEntity.getPaidTime() ? now : creditOrderEntity.getPaidTime());
        creditRepository.saveCreditOrder(creditOrderEntity);

        grantDirectPurchaseCredits(creditOrderEntity);
        return creditRepository.queryCreditOrderByOutTradeNo(creditOrderEntity.getOutTradeNo());
    }

    private void grantDirectPurchaseCredits(CreditOrderEntity creditOrderEntity) {
        creditRepository.grantCredits(CreditGrantEntity.builder()
                .userId(creditOrderEntity.getUserId())
                .teamId(null)
                .outTradeNo(creditOrderEntity.getOutTradeNo())
                .credits(creditOrderEntity.getCredits())
                .grantType(CreditGrantTypeEnumVO.DIRECT_PURCHASE)
                .grantTime(LocalDateTime.now())
                .build());
    }

    private void syncCreditOrderAfterPayIfNecessary(CreditOrderEntity existedCreditOrder, CreditOrderEntity newCreditOrder) {
        if (isBlank(newCreditOrder.getTeamId())) {
            return;
        }

        CreditOrderStatusEnumVO nextStatus = null == newCreditOrder.getStatus()
                ? CreditOrderStatusEnumVO.WAIT_GROUP
                : newCreditOrder.getStatus();
        if (CreditOrderStatusEnumVO.CREDIT_GRANTED == existedCreditOrder.getStatus()) {
            return;
        }

        newCreditOrder.setStatus(nextStatus);
        if (null == newCreditOrder.getPaidTime()) {
            newCreditOrder.setPaidTime(LocalDateTime.now());
        }

        creditRepository.syncCreditOrderAfterPay(newCreditOrder);
    }

    @Override
    public CreditOrderEntity queryCreditOrder(String outTradeNo) {
        if (isBlank(outTradeNo)) {
            throw new IllegalArgumentException("outTradeNo is required");
        }

        return creditRepository.queryCreditOrderByOutTradeNo(outTradeNo.trim());
    }

    @Override
    public List<CreditOrderEntity> queryCreditOrderList(String userId) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }

        return creditRepository.queryCreditOrderListByUserId(userId.trim());
    }

    @Override
    public CreditAccountEntity queryCreditAccount(String userId) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }

        return creditRepository.queryCreditAccount(userId.trim());
    }

    @Override
    public CreditUseEntity consumeCredits(String userId, String requestId, String agentId, String sessionId, long durationMs, String remark) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        if (isBlank(requestId)) {
            throw new IllegalArgumentException("requestId is required");
        }
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs must be greater than or equal to zero");
        }

        BigDecimal usedCredits = BigDecimal.valueOf(Math.max(1, (long) Math.ceil(durationMs / 10000.0D)));
        CreditAccountEntity creditAccountEntity = creditRepository.queryCreditAccount(userId.trim());
        if (null == creditAccountEntity
                || null == creditAccountEntity.getAvailableCredits()
                || creditAccountEntity.getAvailableCredits().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("available credits are insufficient");
        }

        if (creditAccountEntity.getAvailableCredits().compareTo(usedCredits) < 0) {
            usedCredits = creditAccountEntity.getAvailableCredits();
        }

        return creditRepository.consumeCredits(CreditUseEntity.builder()
                .userId(userId.trim())
                .requestId(requestId.trim())
                .agentId(isBlank(agentId) ? null : agentId.trim())
                .sessionId(isBlank(sessionId) ? null : sessionId.trim())
                .durationMs(durationMs)
                .usedCredits(usedCredits)
                .useStatus("SUCCESS")
                .remark(remark)
                .build());
    }

    @Override
    public CreditGrantResultEntity grantCreditsByGroupBuySuccess(GroupBuyTeamSuccessEntity groupBuyTeamSuccessEntity) {
        if (null == groupBuyTeamSuccessEntity || isBlank(groupBuyTeamSuccessEntity.getTeamId())) {
            throw new IllegalArgumentException("teamId is required");
        }

        String teamId = groupBuyTeamSuccessEntity.getTeamId().trim();
        CreditGrantResultEntity result = CreditGrantResultEntity.builder()
                .teamId(teamId)
                .build();

        Set<String> outTradeNoSet = buildOutTradeNoSet(groupBuyTeamSuccessEntity.getOutTradeNoList());
        if (outTradeNoSet.isEmpty()) {
            return result;
        }

        for (String outTradeNo : outTradeNoSet) {
            try {
                CreditOrderEntity creditOrderEntity = creditRepository.queryCreditOrderByOutTradeNo(outTradeNo);
                if (null == creditOrderEntity) {
                    result.addFailed(outTradeNo);
                    continue;
                }

                if (CreditOrderStatusEnumVO.CREDIT_GRANTED == creditOrderEntity.getStatus()
                        || creditRepository.isCreditGranted(outTradeNo)) {
                    result.addSkipped(outTradeNo);
                    continue;
                }

                if (null == creditOrderEntity.getCredits() || creditOrderEntity.getCredits().compareTo(BigDecimal.ZERO) <= 0) {
                    markGrantFailedQuietly(outTradeNo);
                    result.addFailed(outTradeNo);
                    continue;
                }

                creditRepository.markGroupSuccess(outTradeNo);
                creditRepository.grantCredits(CreditGrantEntity.builder()
                        .userId(creditOrderEntity.getUserId())
                        .teamId(teamId)
                        .outTradeNo(outTradeNo)
                        .credits(creditOrderEntity.getCredits())
                        .grantType(CreditGrantTypeEnumVO.GROUP_BUY)
                        .grantTime(LocalDateTime.now())
                        .build());

                result.addGranted(outTradeNo);
            } catch (Exception e) {
                markGrantFailedQuietly(outTradeNo);
                result.addFailed(outTradeNo);
            }
        }

        return result;
    }

    private void checkCreditOrder(CreditOrderEntity creditOrderEntity) {
        if (null == creditOrderEntity) {
            throw new IllegalArgumentException("creditOrderEntity is required");
        }
        if (isBlank(creditOrderEntity.getUserId())) {
            throw new IllegalArgumentException("userId is required");
        }
        if (isBlank(creditOrderEntity.getOutTradeNo())) {
            throw new IllegalArgumentException("outTradeNo is required");
        }
        if (null == creditOrderEntity.getCredits() || creditOrderEntity.getCredits().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("credits must be greater than zero");
        }

        creditOrderEntity.setUserId(creditOrderEntity.getUserId().trim());
        creditOrderEntity.setOutTradeNo(creditOrderEntity.getOutTradeNo().trim());
        if (!isBlank(creditOrderEntity.getTeamId())) {
            creditOrderEntity.setTeamId(creditOrderEntity.getTeamId().trim());
        }
        if (!isBlank(creditOrderEntity.getOrderId())) {
            creditOrderEntity.setOrderId(creditOrderEntity.getOrderId().trim());
        }
        if (!isBlank(creditOrderEntity.getGoodsId())) {
            creditOrderEntity.setGoodsId(creditOrderEntity.getGoodsId().trim());
        }
    }

    private void checkSameCreditOrder(CreditOrderEntity existedCreditOrder, CreditOrderEntity newCreditOrder) {
        if (!Objects.equals(existedCreditOrder.getUserId(), newCreditOrder.getUserId())
                || isDifferentAmount(existedCreditOrder.getCredits(), newCreditOrder.getCredits())) {
            throw new IllegalArgumentException("outTradeNo already exists with different credit order");
        }
    }

    private Set<String> buildOutTradeNoSet(List<String> outTradeNoList) {
        Set<String> outTradeNoSet = new LinkedHashSet<>();
        if (null == outTradeNoList || outTradeNoList.isEmpty()) {
            return outTradeNoSet;
        }

        for (String outTradeNo : outTradeNoList) {
            if (!isBlank(outTradeNo)) {
                outTradeNoSet.add(outTradeNo.trim());
            }
        }

        return outTradeNoSet;
    }

    private boolean isDifferentAmount(BigDecimal oldAmount, BigDecimal newAmount) {
        if (null == oldAmount || null == newAmount) {
            return !Objects.equals(oldAmount, newAmount);
        }

        return oldAmount.compareTo(newAmount) != 0;
    }

    private void markGrantFailedQuietly(String outTradeNo) {
        try {
            creditRepository.markGrantFailed(outTradeNo);
        } catch (Exception ignored) {
            // 批量发放时，失败状态写入异常不能影响后续订单继续处理。
        }
    }

    private boolean isBlank(String value) {
        return null == value || value.trim().isEmpty();
    }

}
