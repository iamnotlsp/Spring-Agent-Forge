package com.lsp.infrastructure.adapter.repository;

import com.lsp.domain.credit.adapter.repository.ICreditRepository;
import com.lsp.domain.credit.model.entity.CreditAccountEntity;
import com.lsp.domain.credit.model.entity.CreditGrantEntity;
import com.lsp.domain.credit.model.entity.CreditOrderEntity;
import com.lsp.domain.credit.model.entity.CreditUseEntity;
import com.lsp.domain.credit.model.valobj.enums.CreditOrderStatusEnumVO;
import com.lsp.infrastructure.dao.ICreditAccountDao;
import com.lsp.infrastructure.dao.ICreditGrantRecordDao;
import com.lsp.infrastructure.dao.ICreditOrderDao;
import com.lsp.infrastructure.dao.ICreditUseRecordDao;
import com.lsp.infrastructure.dao.IGroupBuyProgressDao;
import com.lsp.infrastructure.dao.po.CreditAccount;
import com.lsp.infrastructure.dao.po.CreditGrantRecord;
import com.lsp.infrastructure.dao.po.CreditOrder;
import com.lsp.infrastructure.dao.po.CreditUseRecord;
import com.lsp.infrastructure.dao.po.GroupBuyProgress;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class CreditRepository implements ICreditRepository {

    @Resource
    private ICreditAccountDao creditAccountDao;

    @Resource
    private ICreditOrderDao creditOrderDao;

    @Resource
    private ICreditGrantRecordDao creditGrantRecordDao;

    @Resource
    private ICreditUseRecordDao creditUseRecordDao;

    @Resource
    private IGroupBuyProgressDao groupBuyProgressDao;

    @Override
    public CreditAccountEntity queryCreditAccount(String userId) {
        return buildCreditAccountEntity(creditAccountDao.queryCreditAccount(userId));
    }

    @Override
    public CreditOrderEntity queryCreditOrderByOutTradeNo(String outTradeNo) {
        return enrichGroupBuyProgress(buildCreditOrderEntity(creditOrderDao.queryCreditOrderByOutTradeNo(outTradeNo)));
    }

    @Override
    public List<CreditOrderEntity> queryCreditOrderListByUserId(String userId) {
        List<CreditOrder> creditOrderList = creditOrderDao.queryCreditOrderListByUserId(userId);
        if (null == creditOrderList || creditOrderList.isEmpty()) {
            return Collections.emptyList();
        }

        List<CreditOrderEntity> creditOrderEntityList = creditOrderList.stream()
                .map(this::buildCreditOrderEntity)
                .collect(Collectors.toList());

        enrichGroupBuyProgress(creditOrderEntityList);
        return creditOrderEntityList;
    }

    @Override
    public void saveCreditOrder(CreditOrderEntity creditOrderEntity) {
        creditOrderDao.insert(buildCreditOrder(creditOrderEntity));
        creditAccountDao.insertIgnore(CreditAccount.builder()
                .userId(creditOrderEntity.getUserId())
                .availableCredits(BigDecimal.ZERO)
                .frozenCredits(BigDecimal.ZERO)
                .totalGrantedCredits(BigDecimal.ZERO)
                .totalUsedCredits(BigDecimal.ZERO)
                .status("NORMAL")
                .build());
    }

    @Override
    public void syncCreditOrderAfterPay(CreditOrderEntity creditOrderEntity) {
        creditOrderDao.syncAfterPay(buildCreditOrder(creditOrderEntity));
    }

    @Override
    public boolean isCreditGranted(String outTradeNo) {
        return null != creditGrantRecordDao.queryGrantedRecord(outTradeNo);
    }

    @Override
    public void markGroupSuccess(String outTradeNo) {
        creditOrderDao.updateStatusGroupSuccess(outTradeNo);
    }

    @Override
    public void markGrantFailed(String outTradeNo) {
        creditOrderDao.updateStatusGrantFailed(outTradeNo);
    }

    @Transactional(timeout = 500)
    @Override
    public void grantCredits(CreditGrantEntity creditGrantEntity) {
        try {
            CreditGrantRecord creditGrantRecord = new CreditGrantRecord();
            creditGrantRecord.setUserId(creditGrantEntity.getUserId());
            creditGrantRecord.setTeamId(creditGrantEntity.getTeamId());
            creditGrantRecord.setOutTradeNo(creditGrantEntity.getOutTradeNo());
            creditGrantRecord.setCredits(creditGrantEntity.getCredits());
            creditGrantRecord.setGrantType(creditGrantEntity.getGrantType().getCode());
            creditGrantRecord.setGrantStatus("SUCCESS");
            creditGrantRecord.setGrantTime(creditGrantEntity.getGrantTime());
            creditGrantRecordDao.insert(creditGrantRecord);
        } catch (DuplicateKeyException ignored) {
            return;
        }

        CreditAccount creditAccount = new CreditAccount();
        creditAccount.setUserId(creditGrantEntity.getUserId());
        creditAccount.setAvailableCredits(creditGrantEntity.getCredits());
        creditAccount.setTotalGrantedCredits(creditGrantEntity.getCredits());
        creditAccountDao.addAvailableCredits(creditAccount);

        CreditOrder creditOrder = new CreditOrder();
        creditOrder.setOutTradeNo(creditGrantEntity.getOutTradeNo());
        creditOrder.setTeamId(creditGrantEntity.getTeamId());
        creditOrder.setGrantTime(creditGrantEntity.getGrantTime());
        creditOrderDao.updateStatusCreditGranted(creditOrder);
    }

    @Transactional(timeout = 500)
    @Override
    public CreditUseEntity consumeCredits(CreditUseEntity creditUseEntity) {
        CreditUseRecord existedRecord = creditUseRecordDao.queryByRequestId(creditUseEntity.getRequestId());
        if (null != existedRecord) {
            CreditAccount creditAccount = creditAccountDao.queryCreditAccount(existedRecord.getUserId());
            CreditUseEntity existedUseEntity = buildCreditUseEntity(existedRecord);
            if (null != creditAccount) {
                existedUseEntity.setRemainingCredits(creditAccount.getAvailableCredits());
            }
            return existedUseEntity;
        }

        CreditUseRecord creditUseRecord = new CreditUseRecord();
        creditUseRecord.setUserId(creditUseEntity.getUserId());
        creditUseRecord.setRequestId(creditUseEntity.getRequestId());
        creditUseRecord.setAgentId(creditUseEntity.getAgentId());
        creditUseRecord.setSessionId(creditUseEntity.getSessionId());
        creditUseRecord.setDurationMs(creditUseEntity.getDurationMs());
        creditUseRecord.setUsedCredits(creditUseEntity.getUsedCredits());
        creditUseRecord.setUseStatus(creditUseEntity.getUseStatus());
        creditUseRecord.setRemark(creditUseEntity.getRemark());
        creditUseRecordDao.insert(creditUseRecord);

        CreditAccount deductCreditAccount = new CreditAccount();
        deductCreditAccount.setUserId(creditUseEntity.getUserId());
        deductCreditAccount.setAvailableCredits(creditUseEntity.getUsedCredits());
        deductCreditAccount.setTotalUsedCredits(creditUseEntity.getUsedCredits());
        int updated = creditAccountDao.deductAvailableCredits(deductCreditAccount);
        if (updated <= 0) {
            throw new IllegalStateException("available credits are insufficient");
        }

        CreditAccount creditAccount = creditAccountDao.queryCreditAccount(creditUseEntity.getUserId());
        CreditUseEntity result = buildCreditUseEntity(creditUseRecord);
        if (null != creditAccount) {
            result.setRemainingCredits(creditAccount.getAvailableCredits());
        }
        return result;
    }

    private CreditAccountEntity buildCreditAccountEntity(CreditAccount creditAccount) {
        if (null == creditAccount) {
            return null;
        }

        return CreditAccountEntity.builder()
                .userId(creditAccount.getUserId())
                .availableCredits(creditAccount.getAvailableCredits())
                .frozenCredits(creditAccount.getFrozenCredits())
                .totalGrantedCredits(creditAccount.getTotalGrantedCredits())
                .totalUsedCredits(creditAccount.getTotalUsedCredits())
                .status(creditAccount.getStatus())
                .createTime(creditAccount.getCreateTime())
                .updateTime(creditAccount.getUpdateTime())
                .build();
    }

    private CreditOrderEntity buildCreditOrderEntity(CreditOrder creditOrder) {
        if (null == creditOrder) {
            return null;
        }

        return CreditOrderEntity.builder()
                .userId(creditOrder.getUserId())
                .teamId(creditOrder.getTeamId())
                .orderId(creditOrder.getOrderId())
                .outTradeNo(creditOrder.getOutTradeNo())
                .goodsId(creditOrder.getGoodsId())
                .goodsName(creditOrder.getGoodsName())
                .credits(creditOrder.getCredits())
                .payPrice(creditOrder.getPayPrice())
                .status(CreditOrderStatusEnumVO.valueOfCode(creditOrder.getStatus()))
                .paidTime(creditOrder.getPaidTime())
                .grantTime(creditOrder.getGrantTime())
                .createTime(creditOrder.getCreateTime())
                .updateTime(creditOrder.getUpdateTime())
                .build();
    }

    private CreditUseEntity buildCreditUseEntity(CreditUseRecord creditUseRecord) {
        if (null == creditUseRecord) {
            return null;
        }

        return CreditUseEntity.builder()
                .userId(creditUseRecord.getUserId())
                .requestId(creditUseRecord.getRequestId())
                .agentId(creditUseRecord.getAgentId())
                .sessionId(creditUseRecord.getSessionId())
                .durationMs(creditUseRecord.getDurationMs())
                .usedCredits(creditUseRecord.getUsedCredits())
                .useStatus(creditUseRecord.getUseStatus())
                .remark(creditUseRecord.getRemark())
                .createTime(creditUseRecord.getCreateTime())
                .updateTime(creditUseRecord.getUpdateTime())
                .build();
    }

    private CreditOrderEntity enrichGroupBuyProgress(CreditOrderEntity creditOrderEntity) {
        if (null == creditOrderEntity || isBlank(creditOrderEntity.getTeamId())) {
            return creditOrderEntity;
        }

        enrichGroupBuyProgress(Collections.singletonList(creditOrderEntity));
        return creditOrderEntity;
    }

    private void enrichGroupBuyProgress(List<CreditOrderEntity> creditOrderEntityList) {
        if (null == creditOrderEntityList || creditOrderEntityList.isEmpty()) {
            return;
        }

        Set<String> teamIds = creditOrderEntityList.stream()
                .map(CreditOrderEntity::getTeamId)
                .filter(teamId -> !isBlank(teamId))
                .collect(Collectors.toSet());
        if (teamIds.isEmpty()) {
            return;
        }

        List<GroupBuyProgress> groupBuyProgressList = groupBuyProgressDao.queryGroupBuyProgressByTeamIds(teamIds);
        if (null == groupBuyProgressList || groupBuyProgressList.isEmpty()) {
            return;
        }

        Map<String, GroupBuyProgress> groupBuyProgressMap = groupBuyProgressList.stream()
                .filter(groupBuyProgress -> !isBlank(groupBuyProgress.getTeamId()))
                .collect(Collectors.toMap(GroupBuyProgress::getTeamId, groupBuyProgress -> groupBuyProgress, (left, right) -> left));

        creditOrderEntityList.forEach(creditOrderEntity -> {
            GroupBuyProgress groupBuyProgress = groupBuyProgressMap.get(creditOrderEntity.getTeamId());
            if (null == groupBuyProgress) {
                return;
            }

            int targetCount = normalizeCount(groupBuyProgress.getTargetCount());
            int completeCount = normalizeCount(groupBuyProgress.getCompleteCount());
            creditOrderEntity.setTargetCount(targetCount);
            creditOrderEntity.setJoinedCount(Math.min(targetCount, completeCount));
            creditOrderEntity.setTeamStatus(groupBuyProgress.getStatus());
            if (CreditOrderStatusEnumVO.CREDIT_GRANTED != creditOrderEntity.getStatus()) {
                if (targetCount > 0 && completeCount >= targetCount) {
                    creditOrderEntity.setStatus(CreditOrderStatusEnumVO.GROUP_SUCCESS);
                } else {
                    creditOrderEntity.setStatus(CreditOrderStatusEnumVO.WAIT_GROUP);
                }
            }
        });
    }

    private int normalizeCount(Integer value) {
        return Math.max(0, Objects.requireNonNullElse(value, 0));
    }

    private boolean isBlank(String value) {
        return null == value || value.trim().isEmpty();
    }

    private CreditOrder buildCreditOrder(CreditOrderEntity creditOrderEntity) {
        CreditOrder creditOrder = new CreditOrder();
        creditOrder.setUserId(creditOrderEntity.getUserId());
        creditOrder.setTeamId(creditOrderEntity.getTeamId());
        creditOrder.setOrderId(creditOrderEntity.getOrderId());
        creditOrder.setOutTradeNo(creditOrderEntity.getOutTradeNo());
        creditOrder.setGoodsId(creditOrderEntity.getGoodsId());
        creditOrder.setGoodsName(creditOrderEntity.getGoodsName());
        creditOrder.setCredits(creditOrderEntity.getCredits());
        creditOrder.setPayPrice(creditOrderEntity.getPayPrice());
        creditOrder.setStatus(creditOrderEntity.getStatus().getCode());
        creditOrder.setPaidTime(creditOrderEntity.getPaidTime());
        return creditOrder;
    }

}
