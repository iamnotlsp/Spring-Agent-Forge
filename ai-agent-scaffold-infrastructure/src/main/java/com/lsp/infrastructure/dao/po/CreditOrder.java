package com.lsp.infrastructure.dao.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreditOrder {

    private Long id;

    private String userId;

    private String teamId;

    private String orderId;

    private String outTradeNo;

    private String goodsId;

    private String goodsName;

    private BigDecimal credits;

    private BigDecimal payPrice;

    private String status;

    private LocalDateTime paidTime;

    private LocalDateTime grantTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
