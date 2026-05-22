package com.lsp.domain.agent.service.armory;

import cn.bugstack.wrench.design.framework.tree.AbstractMultiThreadStrategyRouter;
import com.lsp.domain.agent.model.entity.ArmoryCommandEntity;
import com.lsp.domain.agent.model.valobj.AiAgentRegisterVO;
import com.lsp.domain.agent.service.armory.factory.DefaultArmoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @description 装配节点的抽象父类，每添加一个装配节点都要继承这个类即可
 * 底层实现了多线程策略路由器（直接调包了）
 * @author 林善鹏
 * @date 2026-05-21 15:45
 */

public abstract class AbstractArmorySupport extends AbstractMultiThreadStrategyRouter<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> {

    protected final Logger log = LoggerFactory.getLogger(AbstractArmorySupport.class);

    /** 注入 Spring 容器 */
    @Resource
    protected ApplicationContext applicationContext;

    @Override
    protected void multiThread(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {

    }

    /** 根据 Bean 名称，从 Spring 容器中取出对应对象
     *  这样所有规则树节点就能根据 Bean 名称动态获取 Spring 容器里的节点对象
     * */

    protected <T> T getBean(String beanName) {
        return (T) applicationContext.getBean(beanName);
    }

}
