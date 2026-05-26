package com.lsp.domain.agent.service.armory.mcp.client.impl;

import com.lsp.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.lsp.domain.agent.service.armory.mcp.client.TooMcpCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @description 把本项目 Spring 容器里的本地工具，转换成大模型可调用的 ToolCallback[]
 * @author 林善鹏
 * @date 2026-05-26 16:20
 */
@Slf4j
@Service
public class LocalToolMcpCreateService  implements TooMcpCreateService {

    @Resource
    protected ApplicationContext applicationContext;

    @Override
    public ToolCallback[] buildToolCallback(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) {
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.LocalParameters local = toolMcp.getLocal();
        String name = local.getName();

        ToolCallbackProvider localToolCallbackProvider = (ToolCallbackProvider) applicationContext.getBean(local.getName());
        log.info("tool local mcp initialize {}", name);

        return localToolCallbackProvider.getToolCallbacks();
    }

}
