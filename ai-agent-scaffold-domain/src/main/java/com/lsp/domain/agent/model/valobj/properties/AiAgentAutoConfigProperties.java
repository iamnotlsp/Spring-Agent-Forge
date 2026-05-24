package com.lsp.domain.agent.model.valobj.properties;

import com.lsp.domain.agent.model.valobj.AiAgentConfigTableVO;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
//Spring Boot 自动读取 ai.agent.config 开头的 YAML 配置，并注册成 AiAgentAutoConfigProperties 对象
@ConfigurationProperties(prefix = "ai.agent.config", ignoreInvalidFields = true)
public class AiAgentAutoConfigProperties {
    /**
     * 是否启用AI Agent自动装配
     */
    private boolean enabled = false;

    private Map<String, AiAgentConfigTableVO> tables;
}
