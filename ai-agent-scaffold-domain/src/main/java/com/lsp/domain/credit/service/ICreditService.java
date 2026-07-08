package com.lsp.domain.credit.service;

import com.lsp.domain.credit.model.entity.CreditAccountEntity;
import com.lsp.domain.credit.model.entity.CreditGrantResultEntity;
import com.lsp.domain.credit.model.entity.CreditOrderEntity;
import com.lsp.domain.credit.model.entity.CreditUseEntity;
import com.lsp.domain.credit.model.entity.GroupBuyTeamSuccessEntity;

import java.util.List;

/**
 * 额度服务接口，负责承接拼团成团后的额度订单查询、额度账户查询以及额度发放等领域能力。
 */
public interface ICreditService {

    /**
     * 创建额度订单。
     * <p>
     * 一般在用户完成拼团锁单/支付流程后调用，用于记录 outTradeNo 与本系统额度资产的映射关系。
     *
     * @param creditOrderEntity 额度订单实体
     */
    void createCreditOrder(CreditOrderEntity creditOrderEntity);

    /**
     * 创建普通购买额度订单并立即发放额度。
     *
     * @param creditOrderEntity 额度订单实体
     * @return 已完成发放的额度订单
     */
    CreditOrderEntity purchaseCreditOrder(CreditOrderEntity creditOrderEntity);

    /**
     * 根据外部交易单号查询额度订单。
     *
     * @param outTradeNo 外部交易单号
     * @return 额度订单实体
     */
    CreditOrderEntity queryCreditOrder(String outTradeNo);

    /**
     * 查询用户额度订单列表。
     *
     * @param userId 用户ID
     * @return 用户最近的额度订单列表
     */
    List<CreditOrderEntity> queryCreditOrderList(String userId);

    /**
     * 查询用户额度账户。
     *
     * @param userId 用户ID
     * @return 用户额度账户实体
     */
    CreditAccountEntity queryCreditAccount(String userId);

    CreditUseEntity consumeCredits(String userId, String requestId, String agentId, String sessionId, long durationMs, String remark);

    /**
     * 根据拼团成团消息批量发放额度。
     * <p>
     * 该方法由 MQ 消费者或补偿任务调用，内部需要依赖仓储层保证 outTradeNo 幂等，避免重复发放额度。
     *
     * @param groupBuyTeamSuccessEntity 拼团成团消息实体
     * @return 额度发放执行结果
     */
    CreditGrantResultEntity grantCreditsByGroupBuySuccess(GroupBuyTeamSuccessEntity groupBuyTeamSuccessEntity);

}
