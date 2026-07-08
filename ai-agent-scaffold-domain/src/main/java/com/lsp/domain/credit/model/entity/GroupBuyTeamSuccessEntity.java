package com.lsp.domain.credit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 拼团成团消息实体，对应 group-buy-market 发出的 team_success 消息。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyTeamSuccessEntity {

    private String teamId;

    private List<String> outTradeNoList;

}
