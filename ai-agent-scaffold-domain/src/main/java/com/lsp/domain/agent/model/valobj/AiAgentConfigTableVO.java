package com.lsp.domain.agent.model.valobj;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @description Ai Agent 智能体配置表值对象
 * @author 林善鹏
 * @date 2026-05-21 14:19
 */

@Data
public class AiAgentConfigTableVO {
    /**
     * 应用名称
     */
    private String appName;

    /**
     * 智能体配置
     */
    private Agent agent;

    /**
     * 智能体模块
     */
    private Module module;

    /**
     * 主智能体的基础信息
     */
    @Data
    public static class Agent {

        /**
         * 智能体ID
         */
        private String agentId;

        /**
         * 智能体名称
         */
        private String agentName;

        /**
         * 智能体描述
         */
        private String agentDesc;

    }

    /**
     * 智能体具体怎么工作
     */
    @Data
    public static class Module {

        private AiApi aiApi;

        private ChatModel chatModel;

        private List<Agent> agents;

        private List<AgentWorkflow> agentWorkflows;

        //大模型基础配置
        @Data
        public static class AiApi {
            private String baseUrl;
            private String apiKey;
            private String completionsPath = "/v1/chat/completions";
            private String embeddingsPath = "/v1/embeddings";

        }

        /**
         * 工具配置，支持两种 MCP 接入方式
         * SSE   远程 MCP 服务，比如百度搜索 MCP
         * Stdio 本地 MCP 服务，比如自己写的本地工具服务
         */
        @Data
        public static class ChatModel {

            private String model;
            private List<ToolMcp> toolMcpList;

            @Data
            public static class ToolMcp {

                private SSEServerParameters sse;

                private StdioServerParameters stdio;

                @Data
                public static class SSEServerParameters {
                    private String name;
                    private String baseUri;
                    private String sseEndpoint;
                    private Integer requestTimeout = 3000;

                }

                @Data
                public static class StdioServerParameters {
                    private String name;
                    private Integer requestTimeout = 3000;
                    private ServerParameters serverParameters;

                    @Data
                    public static class ServerParameters {
                        private String command;
                        private List<String> args;
                        private Map<String, String> env;

                    }
                }

            }
        }

        /**
         * 实际参与编排的子智能体
         * 比如写作 Agent、检索 Agent、总结 Agent 等
         */
        @Data
        public static class Agent {
            private String name;
            private String instruction;
            private String description;
            private String outputKey;

        }

        /**
         * 智能体工作流配置 定义多个子 Agent 怎么组合
         * 包括循环（loop）、并行（parallel）、顺序（sequential）等类型
         */
        @Data
        public static class AgentWorkflow {
            /**
             * 类型；loop、parallel、sequential
             */
            private String type;
            private String name;
            private List<String> subAgents;
            private String description;
            private Integer maxIterations = 3;

        }
    }
}
