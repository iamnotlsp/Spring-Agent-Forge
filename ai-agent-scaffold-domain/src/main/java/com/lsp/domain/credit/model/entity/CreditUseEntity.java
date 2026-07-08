package com.lsp.domain.credit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditUseEntity {

    private String userId;

    private String requestId;

    private String agentId;

    private String sessionId;

    private Long durationMs;

    private BigDecimal usedCredits;

    private BigDecimal remainingCredits;

    private String useStatus;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
