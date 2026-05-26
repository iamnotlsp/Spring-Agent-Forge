package com.lsp.domain.agent.service.armory.tool.mcp.client;

import com.lsp.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.springframework.ai.tool.ToolCallback;

/**
 * @description 工具 MCP 构建服务
 * @author 林善鹏
 * @date 2026-05-26 15:38
 */
public interface TooMcpCreateService {
    ToolCallback[] buildToolCallback(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception;
}
