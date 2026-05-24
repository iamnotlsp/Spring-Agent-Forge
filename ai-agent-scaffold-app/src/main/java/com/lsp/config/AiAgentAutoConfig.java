package com.lsp.config;

import com.alibaba.fastjson.JSON;
import com.lsp.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import com.lsp.domain.agent.service.IArmoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.ArrayList;

@Slf4j
@Configuration
@EnableConfigurationProperties(AiAgentAutoConfigProperties.class)
public class AiAgentAutoConfig implements ApplicationListener<ApplicationReadyEvent> {
    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;

    @Resource
    private IArmoryService armoryService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            log.info("Ai Agent 智能体装配 {}", JSON.toJSONString(aiAgentAutoConfigProperties.getTables().values()));

            /**
             * 1. 从spring容器中拿到 AiAgentAutoConfigProperties
             * 2. 遍历 AiAgentAutoConfigProperties中每一份智能体配置
             * 3. 为每一个智能体搭建一个上下文，并走同一套装配链
             */
            armoryService.acceptArmoryAgents(new ArrayList<>(aiAgentAutoConfigProperties.getTables().values()));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
