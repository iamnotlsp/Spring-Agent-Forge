package com.lsp.domain.credit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 拼团成团批量发放额度的执行结果。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditGrantResultEntity {

    private String teamId;

    @Builder.Default
    private List<String> grantedOutTradeNoList = new ArrayList<>();

    @Builder.Default
    private List<String> skippedOutTradeNoList = new ArrayList<>();

    @Builder.Default
    private List<String> failedOutTradeNoList = new ArrayList<>();

    public void addGranted(String outTradeNo) {
        this.grantedOutTradeNoList.add(outTradeNo);
    }

    public void addSkipped(String outTradeNo) {
        this.skippedOutTradeNoList.add(outTradeNo);
    }

    public void addFailed(String outTradeNo) {
        this.failedOutTradeNoList.add(outTradeNo);
    }

}
