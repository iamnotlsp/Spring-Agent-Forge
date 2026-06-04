package com.lsp.domain.agent.service.armory.tool.skills;

import com.lsp.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.springframework.ai.tool.ToolCallback;

/**
 * @description 工具 skills 构建服务
 * @author 林善鹏
 * @date 2026-06-04 14:53
 */
public interface ToolSkillsCreateService {

    ToolCallback[] buildToolCallback(AiAgentConfigTableVO.Module.ChatModel.ToolSkills toolSkills) throws Exception;

}
