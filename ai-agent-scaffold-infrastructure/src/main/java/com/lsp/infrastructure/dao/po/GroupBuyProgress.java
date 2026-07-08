package com.lsp.infrastructure.dao.po;

import lombok.Data;

@Data
public class GroupBuyProgress {

    private String teamId;

    private Integer targetCount;

    private Integer completeCount;

    private Integer lockCount;

    private Integer status;

}
