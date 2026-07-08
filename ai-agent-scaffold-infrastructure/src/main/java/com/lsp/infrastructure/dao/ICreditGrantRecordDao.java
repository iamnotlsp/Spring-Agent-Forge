package com.lsp.infrastructure.dao;

import com.lsp.infrastructure.dao.po.CreditGrantRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ICreditGrantRecordDao {

    CreditGrantRecord queryGrantedRecord(@Param("outTradeNo") String outTradeNo);

    int insert(CreditGrantRecord creditGrantRecord);

}
