package com.lsp.domain.agent.service.armory.node.workflow;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LoopAgent;
import com.lsp.domain.agent.model.entity.ArmoryCommandEntity;
import com.lsp.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.lsp.domain.agent.model.valobj.AiAgentRegisterVO;
import com.lsp.domain.agent.model.valobj.enums.AgentTypeEnum;
import com.lsp.domain.agent.service.armory.AbstractArmorySupport;
import com.lsp.domain.agent.service.armory.factory.DefaultArmoryFactory;
import com.lsp.domain.agent.service.armory.node.AgentWorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


/**
 * @description 循环工作流
 * 一组 agent反复执行，直到满足推出条件 或者 达到最大迭代次数
 * @author 林善鹏
 * @date 2026-05-22 16:20
 */
@Slf4j
@Service("loopAgentNode")
public class LoopAgentNode extends AbstractArmorySupport {

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - LoopAgentNode - 循环工作流");

        // 取出当前工作流
        AiAgentConfigTableVO.Module.AgentWorkflow agentWorkflow = dynamicContext.getCurrentAgentWorkflow();


        // 取出这个 Loop 工作流需要包含哪些子 Agent
        // 子 Agent 通过 test-agent.yml配置文件间接配置
        List<String> subAgentNames = agentWorkflow.getSubAgents();
        List<BaseAgent> subAgents = dynamicContext.queryAgentList(subAgentNames);

        // 构建 LoopAgent
        LoopAgent loopAgent =
                LoopAgent.builder()
                        .name(agentWorkflow.getName())
                        .description(agentWorkflow.getDescription())
                        .subAgents(subAgents)
                        .maxIterations(agentWorkflow.getMaxIterations())
                        .build();

        // 将 LoopAgent 放进 agentGroup，让后续节点可以通过名字找到它
        // 因为工作流 Agent 也可能成为后续另一个工作流的子 Agent
        dynamicContext.getAgentGroup().put(agentWorkflow.getName(), loopAgent);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return getBean("agentWorkflowNode");

    }
}
