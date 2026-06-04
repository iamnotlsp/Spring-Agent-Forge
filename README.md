# Spring Agent Forge

一个基于 Spring AI 的 AI Agent 快速开发脚手架。

它更适合有 Java / Spring Boot 基础、想快速学习 Spring AI 并搭建属于自己智能体应用的开发者。项目把大模型接入、Agent 编排、MCP 工具、Skills 技能、流式对话、Web 调试台这些常用能力提前搭好。之后想做一个新的智能体应用，只需要改 YAML 配置、补业务工具、写少量编排逻辑，就可以快速跑起来。

## 我想用它解决什么

做 AI Agent 项目时，经常会反复处理这些基础工作：

- 接模型：OpenAI 兼容接口、模型参数、API Key、baseUrl
- 接工具：MCP SSE、MCP Stdio、本地工具、远程工具
- 写 Agent：角色、提示词、输出变量、上下文传递
- 做编排：串行、并行、循环、多 Agent 协作
- 调接口：普通对话、流式对话、Session 管理
- 做展示：一个能快速测试 Agent 的前端界面

这个项目的目标就是把这些“每次都要搭一遍”的东西沉淀成脚手架，让后续开发更像是在拼装能力，而不是从零开荒。


## 技术栈

- Java 17
- Spring Boot 3.4.3
- Spring AI 1.1.0-M3
- Google ADK 1.1.0
- Maven 3.8+
- MySQL
- Vue 3
- Vite 6
- TypeScript

## 项目结构

```text
ai-agent-scaffold
├── ai-agent-scaffold-api
├── ai-agent-scaffold-app
├── ai-agent-scaffold-domain
├── ai-agent-scaffold-infrastructure
├── ai-agent-scaffold-trigger
├── ai-agent-scaffold-types
├── ai-agent-scaffold-web
└── docs
```

模块说明：

| 模块 | 说明 |
| --- | --- |
| `ai-agent-scaffold-api` | 对外接口、DTO、响应对象 |
| `ai-agent-scaffold-app` | Spring Boot 启动模块、配置文件、测试用例 |
| `ai-agent-scaffold-domain` | Agent 装配、工作流、对话服务、领域模型 |
| `ai-agent-scaffold-infrastructure` | 基础设施层 |
| `ai-agent-scaffold-trigger` | HTTP Controller 入口 |
| `ai-agent-scaffold-types` | 通用类型、异常、响应码 |
| `ai-agent-scaffold-web` | Vue 前端调试台 |
| `docs` | 部署脚本、接口示例、开发资料 |

## 核心思路

这个项目的核心是：用配置驱动 Agent 装配。

一份 YAML 配置描述一个智能体应用，包括模型、工具、普通 Agent、工作流和 Runner。启动 Spring Boot 后，系统会读取配置，并按装配链自动构建可运行的 Agent。

装配流程大致如下：

```text
YAML 配置
  -> AiAgentConfigTableVO
  -> AiApiNode
  -> ChatModelNode
  -> AgentNode
  -> AgentWorkflowNode
  -> RunnerNode
  -> AiAgentRegisterVO
  -> ChatService / HTTP API
```

节点职责：

| 节点 | 作用 |
| --- | --- |
| `AiApiNode` | 创建 OpenAI 兼容 API 客户端 |
| `ChatModelNode` | 创建 ChatModel，并挂载 MCP、Skills 等工具 |
| `AgentNode` | 根据配置创建普通 LlmAgent |
| `AgentWorkflowNode` | 根据配置创建串行、并行、循环工作流 |
| `RunnerNode` | 创建 InMemoryRunner，并注册到 Spring 容器 |

## Agent 配置

Agent 配置文件放在：

```text
ai-agent-scaffold-app/src/main/resources/agent/
```

在 `application-dev.yml` 中导入：

```yaml
spring:
  config:
    import:
      - classpath:agent/qwen-agent.yml
```

一个典型配置长这样：

```yaml
ai:
  agent:
    config:
      tables:
        exampleAgent:
          app-name: ExampleAgentApp
          agent:
            agent-id: 100001
            agent-name: 示例智能体
            agent-desc: 用于演示的智能体
          module:
            ai-api:
              base-url: https://api.example.com
              api-key: ${AI_API_KEY}
              completions-path: v1/chat/completions
              embeddings-path: v1/embeddings
            chat-model:
              model: example-model
              tool-mcp-list: []
              tool-skills-list:
                - type: resource
                  path: agent/skills
            agents:
              - name: onlyAgent
                description: 示例 Agent
                instruction: |
                  你是一个智能体助手。
                output-key: result
            agent-workflows:
              - type: sequential
                name: MainAgent
                description: 主流程
                sub-agents:
                  - onlyAgent
            runner:
              agent-name: MainAgent
```

## 工作流

目前支持三种常见 Agent 工作流：

| 类型 | 说明 | 适合场景 |
| --- | --- | --- |
| `sequential` | 多个 Agent 按顺序执行 | 写作、审查、重构这类固定步骤任务 |
| `parallel` | 多个 Agent 并行执行，再汇总结果 | 多方向调研、信息收集、方案对比 |
| `loop` | 一组 Agent 循环执行，直到满足退出条件或达到最大次数 | 生成、检查、修改、再检查的迭代任务 |

配置示例：

```yaml
agent-workflows:
  - type: sequential
    name: CodePipelineAgent
    description: 代码生成、审查、重构流程
    sub-agents:
      - CodeWriterAgent
      - CodeReviewerAgent
      - CodeRefactorerAgent
```

## MCP 工具

MCP 可以理解为“给智能体使用外部工具的协议”。

支持两种接入方式：

| 类型 | 说明 |
| --- | --- |
| SSE | 远程 MCP 服务，例如搜索、地图、企业服务 |
| Stdio | 本地 MCP 服务，通过本地进程输入输出通信 |

配置示例：

```yaml
tool-mcp-list:
  - sse:
      name: baidu-search
      base-uri: http://appbuilder.baidu.com/v2/ai_search/mcp/
      sse-endpoint: sse?api_key=${BAIDU_MCP_KEY}
      request-timeout: 500000
```

## Skills 技能

Skills 更像是给 Agent 装“技能说明书”。它可以告诉模型某类任务应该怎么做、按什么步骤做、输出什么格式。

配置示例：

```yaml
tool-skills-list:
  - type: resource
    path: agent/skills
  - type: directory
    path: E:/javaProject/ai-agent-scaffold/skills
```

两种来源：

| 类型 | 说明 |
| --- | --- |
| `resource` | 放在 `src/main/resources` 下，跟随项目打包 |
| `directory` | 放在本地目录，适合开发调试和个人技能库 |

注意：Skills 本身偏提示词和方法论，如果要让 Agent 真正调用外部服务，还需要 MCP 或 ToolCallback 配合。

## 后端启动

先确认本地环境：

- JDK 17
- Maven 3.8+
- MySQL
- 已配置模型 API Key
- 已调整 `application-dev.yml`

编译：

```powershell
mvn clean compile
```

启动：

```text
运行 ai-agent-scaffold-app/src/main/java/com/lsp/Application.java
```

默认端口：

```text
http://127.0.0.1:8091
```

## 前端启动

前端目录：

```powershell
cd ai-agent-scaffold-web
```

安装依赖：

```powershell
npm install
```

启动：

```powershell
npm run dev
```

访问：

```text
http://127.0.0.1:5173/
```

前端默认代理到后端：

```ts
server: {
  proxy: {
    '/api': {
      target: 'http://127.0.0.1:8091',
      changeOrigin: true
    }
  }
}
```

## HTTP API

查询 Agent 列表：

```http
GET /api/v1/query_ai_agent_config_list
```

创建会话：

```http
POST /api/v1/create_session
Content-Type: application/json

{
  "agentId": "100001",
  "userId": "lsp"
}
```

普通对话：

```http
POST /api/v1/chat
Content-Type: application/json

{
  "agentId": "100001",
  "userId": "lsp",
  "sessionId": "session-id",
  "message": "你好"
}
```

流式对话：

```http
POST /api/v1/chat_stream
Accept: text/event-stream
Content-Type: application/json

{
  "agentId": "100001",
  "userId": "lsp",
  "sessionId": "session-id",
  "message": "你好"
}
```

## Web 调试台

`ai-agent-scaffold-web` 是一个轻量级前端调试台：

- 自动加载 Agent 列表
- 选择 Agent 后直接发送消息
- 自动创建会话
- 默认使用 SSE 流式输出
- 适合快速验证 Agent 配置和工具接入

## 插件与监控

项目支持 ADK Plugin 扩展：

| 插件 | 说明 |
| --- | --- |
| `myLogPlugin` | 打印 Agent 执行过程，适合调试 |
| `myTestPlugin` | 打印用户输入、Agent 名称等基础信息 |
| `prometheusMetricsPlugin` | 采集 Agent 调用次数、耗时、工具调用指标 |

流式输出时，`myLogPlugin` 可能会打印大量 partial event 日志。调试前端流式体验时，可以先从 `plugin-name-list` 中移除它。

Prometheus 暴露配置：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

## 开发一个新 Agent 的推荐步骤

1. 新建一份 `agent/xxx-agent.yml`
2. 配置 `ai-api` 和 `chat-model`
3. 编写普通 Agent 的 `instruction`
4. 选择工作流类型：`sequential`、`parallel` 或 `loop`
5. 如需外部工具，配置 `tool-mcp-list`
6. 如需技能说明，配置 `tool-skills-list`
7. 在 `application-dev.yml` 中导入这份配置
8. 启动后端，用 Web 调试台验证效果

