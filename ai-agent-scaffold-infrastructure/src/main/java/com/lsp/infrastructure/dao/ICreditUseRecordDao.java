package com.lsp.infrastructure.dao;

import com.lsp.infrastructure.dao.po.CreditUseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ICreditUseRecordDao {

    CreditUseRecord queryByRequestId(@Param("requestId") String requestId);

    int insert(CreditUseRecord creditUseRecord);

}
