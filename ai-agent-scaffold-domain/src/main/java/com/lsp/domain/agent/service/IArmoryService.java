package com.lsp.domain.agent.service;

import com.lsp.domain.agent.model.valobj.AiAgentConfigTableVO;

import java.util.List;

/**
 * @description 装配接口
 * @author 林善鹏
 * @date 2026-05-21 15:01
 */

public interface IArmoryService {
    void acceptArmoryAgents(List<AiAgentConfigTableVO> tables) throws Exception;
}
