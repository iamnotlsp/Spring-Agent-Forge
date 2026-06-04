package com.lsp.api.dto;

import lombok.Data;

/**
 * @description 智能体配置响应对象
 * @author 林善鹏
 * @date 2026-05-29 16:32
 */

@Data
public class AiAgentConfigResponseDTO {

    /**
     * 智能体ID
     */
    private String agentId;

    /**
     * 智能体名称
     */
    private String agentName;

    /**
     * 智能体描述
     */
    private String agentDesc;

}
