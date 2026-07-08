package com.lsp.domain.credit.model.entity;

import com.lsp.domain.credit.model.valobj.enums.CreditOrderStatusEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 额度订单实体，承接拼团订单与本系统额度资产的映射关系。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditOrderEntity {

    private String userId;

    private String teamId;

    private String orderId;

    private String outTradeNo;

    private String goodsId;

    private String goodsName;

    private BigDecimal credits;

    private BigDecimal payPrice;

    private CreditOrderStatusEnumVO status;

    private LocalDateTime paidTime;

    private LocalDateTime grantTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer targetCount;

    private Integer joinedCount;

    private Integer teamStatus;

}
