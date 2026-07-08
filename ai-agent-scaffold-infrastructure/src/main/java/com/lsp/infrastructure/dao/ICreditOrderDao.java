package com.lsp.infrastructure.dao;

import com.lsp.infrastructure.dao.po.CreditOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ICreditOrderDao {

    CreditOrder queryCreditOrderByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    List<CreditOrder> queryCreditOrderListByUserId(@Param("userId") String userId);

    int insert(CreditOrder creditOrder);

    int syncAfterPay(CreditOrder creditOrder);

    int updateStatusGroupSuccess(@Param("outTradeNo") String outTradeNo);

    int updateStatusCreditGranted(CreditOrder creditOrder);

    int updateStatusGrantFailed(@Param("outTradeNo") String outTradeNo);

}
