package com.lsp.infrastructure.dao.po;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditAccount {

    private Long id;

    private String userId;

    private BigDecimal availableCredits;

    private BigDecimal frozenCredits;

    private BigDecimal totalGrantedCredits;

    private BigDecimal totalUsedCredits;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
