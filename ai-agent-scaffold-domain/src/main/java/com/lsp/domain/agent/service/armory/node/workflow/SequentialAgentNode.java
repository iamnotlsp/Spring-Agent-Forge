package com.lsp.domain.agent.service.armory.node.workflow;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.SequentialAgent;
import com.lsp.domain.agent.model.entity.ArmoryCommandEntity;
import com.lsp.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.lsp.domain.agent.model.valobj.AiAgentRegisterVO;
import com.lsp.domain.agent.service.armory.AbstractArmorySupport;
import com.lsp.domain.agent.service.armory.factory.DefaultArmoryFactory;
import com.lsp.domain.agent.service.armory.node.RunnerNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author 林善鹏
 * @description 串行工作流
 * 多个子 agent按照固定顺序依次执行
 * @date 2026-05-22 16:06
 */

@Slf4j
@Service("sequentialAgentNode")
public class SequentialAgentNode extends AbstractArmorySupport {

    @Resource
    private RunnerNode runnerNode;

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - SequentialAgentNode - 串行工作流");

        // 取出当前工作流
        AiAgentConfigTableVO.Module.AgentWorkflow agentWorkflow = dynamicContext.getCurrentAgentWorkflow();

        // 根据工作流的子智能体配置 从上下文agentGroup取得subAgents
        List<String> subAgentNames = agentWorkflow.getSubAgents();
        List<BaseAgent> subAgents = dynamicContext.queryAgentList(subAgentNames);

        //再根据这些subAgents组合成sequentialAgent
        SequentialAgent sequentialAgent =
                SequentialAgent.builder()
                        .name(agentWorkflow.getName())
                        .description(agentWorkflow.getDescription())
                        .subAgents(subAgents)
                        .build();

        //最后放入上下文agentGroup，供其他工作流使用
        dynamicContext.getAgentGroup().put(agentWorkflow.getName(), sequentialAgent);


        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        // 目前串行工作流作为兜底，到最后执行节点
        return getBean("agentWorkflowNode");
    }

}
