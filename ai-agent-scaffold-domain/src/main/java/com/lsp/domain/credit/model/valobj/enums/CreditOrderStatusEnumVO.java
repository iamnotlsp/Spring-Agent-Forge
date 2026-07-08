package com.lsp.domain.credit.model.valobj.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum CreditOrderStatusEnumVO {

    WAIT_GROUP("WAIT_GROUP", "已支付，等待拼团成团"),
    GROUP_SUCCESS("GROUP_SUCCESS", "拼团已成团，等待额度发放"),
    CREDIT_GRANTED("CREDIT_GRANTED", "额度已发放"),
    GRANT_FAILED("GRANT_FAILED", "额度发放失败"),
    ;

    private String code;
    private String info;

    public static CreditOrderStatusEnumVO valueOfCode(String code) {
        if (null == code) {
            return null;
        }

        for (CreditOrderStatusEnumVO value : values()) {
            if (value.getCode().equalsIgnoreCase(code)) {
                return value;
            }
        }

        return null;
    }

}
