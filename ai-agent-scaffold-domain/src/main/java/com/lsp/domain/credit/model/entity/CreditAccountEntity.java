package com.lsp.domain.credit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户额度账户实体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditAccountEntity {

    private String userId;

    private BigDecimal availableCredits;

    private BigDecimal frozenCredits;

    private BigDecimal totalGrantedCredits;

    private BigDecimal totalUsedCredits;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
