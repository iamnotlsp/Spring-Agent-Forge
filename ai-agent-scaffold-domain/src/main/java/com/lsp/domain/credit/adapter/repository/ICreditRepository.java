package com.lsp.domain.credit.adapter.repository;

import com.lsp.domain.credit.model.entity.CreditAccountEntity;
import com.lsp.domain.credit.model.entity.CreditGrantEntity;
import com.lsp.domain.credit.model.entity.CreditOrderEntity;
import com.lsp.domain.credit.model.entity.CreditUseEntity;

import java.util.List;

/**
 * 额度仓储接口，由基础设施层实现具体持久化能力。
 */
public interface ICreditRepository {

    CreditAccountEntity queryCreditAccount(String userId);

    CreditOrderEntity queryCreditOrderByOutTradeNo(String outTradeNo);

    List<CreditOrderEntity> queryCreditOrderListByUserId(String userId);

    void saveCreditOrder(CreditOrderEntity creditOrderEntity);

    void syncCreditOrderAfterPay(CreditOrderEntity creditOrderEntity);

    boolean isCreditGranted(String outTradeNo);

    void markGroupSuccess(String outTradeNo);

    void markGrantFailed(String outTradeNo);

    void grantCredits(CreditGrantEntity creditGrantEntity);

    CreditUseEntity consumeCredits(CreditUseEntity creditUseEntity);

}
