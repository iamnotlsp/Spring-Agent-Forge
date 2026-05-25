package com.lsp.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.lsp.domain.agent.model.entity.ArmoryCommandEntity;
import com.lsp.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.lsp.domain.agent.model.valobj.AiAgentRegisterVO;
import com.lsp.domain.agent.model.valobj.enums.AgentTypeEnum;
import com.lsp.domain.agent.service.armory.AbstractArmorySupport;
import com.lsp.domain.agent.service.armory.factory.DefaultArmoryFactory;
import com.lsp.domain.agent.service.armory.node.workflow.LoopAgentNode;
import com.lsp.domain.agent.service.armory.node.workflow.ParallelAgentNode;
import com.lsp.domain.agent.service.armory.node.workflow.SequentialAgentNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class AgentWorkflowNode extends AbstractArmorySupport {

    @Resource
    private LoopAgentNode loopAgentNode;
    @Resource
    private ParallelAgentNode parallelAgentNode;
    @Resource
    private SequentialAgentNode sequentialAgentNode;
    @Resource
    private RunnerNode runnerNode;

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - AgentWorkflowNode - 配置多个子智能体工作流方式");

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = aiAgentConfigTableVO.getModule().getAgentWorkflows();

        //没有工作流直接路由到runnerNode节点继续执行
        if (null == agentWorkflows || agentWorkflows.isEmpty() || dynamicContext.getCurrentStepIndex() >= agentWorkflows.size()) {
            // 设置结果值
            dynamicContext.setCurrentAgentWorkflow(null);
            // 路由下节点
            return router(requestParameter, dynamicContext);
        }

        //根据上下文当前步骤得到当前工作流配置，并设置到上下文里
        dynamicContext.setCurrentAgentWorkflow(agentWorkflows.get(dynamicContext.getCurrentStepIndex()));

        // 步骤值增加
        dynamicContext.addCurrentStepIndex();

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        // 从上下文里拿当前的工作流配置
        AiAgentConfigTableVO.Module.AgentWorkflow currentAgentWorkflow = dynamicContext.getCurrentAgentWorkflow();

        if (null == currentAgentWorkflow){
            return runnerNode;
        }

        String type = currentAgentWorkflow.getType();

        AgentTypeEnum agentTypeEnum = AgentTypeEnum.formType(type);

        if (null == agentTypeEnum) {
            throw new RuntimeException("agentWorkflow type is error!");
        }

        // 通过枚举取得对应的node字符串，
        String node = agentTypeEnum.getNode();

        // 最终路由到对应的node上 默认路由到runnerNode上
        return switch (node) {
            case "loopAgentNode" -> loopAgentNode;
            case "parallelAgentNode" -> parallelAgentNode;
            case "sequentialAgentNode" -> sequentialAgentNode;
            default -> runnerNode;
        };
    }
}
