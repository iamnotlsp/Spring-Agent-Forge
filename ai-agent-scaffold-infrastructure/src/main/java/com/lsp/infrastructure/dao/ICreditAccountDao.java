package com.lsp.infrastructure.dao;

import com.lsp.infrastructure.dao.po.CreditAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ICreditAccountDao {

    CreditAccount queryCreditAccount(@Param("userId") String userId);

    int insertIgnore(CreditAccount creditAccount);

    int addAvailableCredits(CreditAccount creditAccount);

    int deductAvailableCredits(CreditAccount creditAccount);

}
