package com.lsp.domain.credit.model.valobj.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum CreditGrantTypeEnumVO {

    GROUP_BUY("GROUP_BUY", "拼团成团发放"),
    DIRECT_PURCHASE("DIRECT_PURCHASE", "原价购买发放"),
    MANUAL("MANUAL", "人工发放"),
    ;

    private String code;
    private String info;

}
