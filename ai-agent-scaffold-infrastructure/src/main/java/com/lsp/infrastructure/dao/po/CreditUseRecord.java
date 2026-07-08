package com.lsp.infrastructure.dao.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreditUseRecord {

    private Long id;

    private String userId;

    private String requestId;

    private String agentId;

    private String sessionId;

    private Long durationMs;

    private BigDecimal usedCredits;

    private String useStatus;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
