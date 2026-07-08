package com.lsp.domain.credit.model.entity;

import com.lsp.domain.credit.model.valobj.enums.CreditGrantTypeEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 额度发放命令实体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditGrantEntity {

    private String userId;

    private String teamId;

    private String outTradeNo;

    private BigDecimal credits;

    private CreditGrantTypeEnumVO grantType;

    private LocalDateTime grantTime;

}
