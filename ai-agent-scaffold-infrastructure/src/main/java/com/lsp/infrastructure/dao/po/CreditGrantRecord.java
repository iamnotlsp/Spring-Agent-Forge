package com.lsp.infrastructure.dao.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreditGrantRecord {

    private Long id;

    private String userId;

    private String teamId;

    private String outTradeNo;

    private BigDecimal credits;

    private String grantType;

    private String grantStatus;

    private LocalDateTime grantTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
