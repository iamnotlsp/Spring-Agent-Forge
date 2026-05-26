package com.lsp.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.lsp.domain.agent.model.entity.ArmoryCommandEntity;
import com.lsp.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.lsp.domain.agent.model.valobj.AiAgentRegisterVO;
import com.lsp.domain.agent.service.armory.AbstractArmorySupport;
import com.lsp.domain.agent.service.armory.factory.DefaultArmoryFactory;
import com.lsp.domain.agent.service.armory.tool.mcp.client.TooMcpCreateService;
import com.lsp.domain.agent.service.armory.tool.mcp.client.factory.DefaultMcpClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChatModelNode extends AbstractArmorySupport {

    @Resource
    private AgentNode agentNode;

    @Resource
    private DefaultMcpClientFactory defaultMcpClientFactory;

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - ChatModelNode - 构建对话模型配置及加入MCP工具至模型配置");

        OpenAiApi openAiApi = dynamicContext.getOpenAiApi();

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        // 得到对话模型配置
        AiAgentConfigTableVO.Module.ChatModel chatModelConfig = aiAgentConfigTableVO.getModule().getChatModel();
        // 得到MCP配置列表
        List<AiAgentConfigTableVO.Module.ChatModel.ToolMcp> toolMcpList = chatModelConfig.getToolMcpList();
        // 构建mcp服务（工厂）
        // ToolCallback 就是 Spring AI 里“模型可以调用的工具”
        List<ToolCallback> toolCallbackList = new ArrayList<>();
        for (AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp : toolMcpList) {
            // 使用工厂将一项配置真正转换成 Spring AI 可识别的工具回调
            TooMcpCreateService tooMcpCreateService = defaultMcpClientFactory.getTooMcpCreateService(toolMcp);
            ToolCallback[] toolCallbacks = tooMcpCreateService.buildToolCallback(toolMcp);
            toolCallbackList.addAll(List.of(toolCallbacks));
        }

        // 构建一个ChatModel（OpenAI 兼容模型）并把LLM api连接 和 MCP工具能力一起挂载进去（使其既能对话又能调用外部工具）
        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatModelConfig.getModel())
                        .toolCallbacks(toolCallbackList)
                        .build())
                .build();

        // 将构建好的 ChatModel 放入动态上下文中，以便后续节点使用
        dynamicContext.setChatModel(chatModel);

        return router(requestParameter, dynamicContext);

    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return agentNode;
    }
//    /**
//     * @description  如果配置了 SSE，就连接远程 MCP；如果配置了 Stdio，就启动本地 MCP；
//     * @author 林善鹏
//     * @date 2026-05-21 16:38
//     */
//    private McpSyncClient createMcpSyncClient(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception {
//
//        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.SSEServerParameters sseConfig = toolMcp.getSse();
//        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters stdioConfig = toolMcp.getStdio();
//
//        if (null != sseConfig) {
//            // http://appbuilder.baidu.com/v2/ai_search/mcp/sse?api_key=bce-v3/ALTAK-JFZXXLpfxhAutDQvJ32Ei/4492c1879b8c2f0df4612ef5b4a52df1c1fba9f7
//
//            String originalBaseUri = sseConfig.getBaseUri();
//            String baseUri = originalBaseUri;
//            String sseEndpoint = sseConfig.getSseEndpoint();
//
//            if (StringUtils.isBlank(sseEndpoint)) {
//                URL url = new URL(originalBaseUri);
//
//                String protocol = url.getProtocol();
//                String host = url.getHost();
//                int port = url.getPort();
//
//                String baseUrl = port == -1 ? protocol + "://" + host : protocol + "://" + host + ":" + port;
//
//                int index = originalBaseUri.indexOf(baseUrl);
//                if (index != -1) {
//                    sseEndpoint = originalBaseUri.substring(index + baseUrl.length());
//                }
//
//                baseUri = baseUrl;
//            }
//
//            sseEndpoint = StringUtils.isBlank(sseEndpoint) ? "/sse" : sseEndpoint;
//
//            HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
//                    .builder(baseUri)
//                    .sseEndpoint(sseEndpoint)
//                    .build();
//
//            McpSyncClient mcpSyncClient = McpClient
//                    .sync(sseClientTransport)
//                    .requestTimeout(Duration.ofMillis(sseConfig.getRequestTimeout())).build();
//            McpSchema.InitializeResult initialize = mcpSyncClient.initialize();
//
//            log.info("tool sse mcp initialize {}", initialize);
//
//            return mcpSyncClient;
//        }
//
//        if (null != stdioConfig) {
//            AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters.ServerParameters serverParameters = stdioConfig.getServerParameters();
//
//            ServerParameters stdioParams = ServerParameters.builder(serverParameters.getCommand())
//                    .args(serverParameters.getArgs())
//                    .env(serverParameters.getEnv())
//                    .build();
//
//            McpSyncClient mcpSyncClient = McpClient.sync(new StdioClientTransport(stdioParams, new JacksonMcpJsonMapper(new ObjectMapper())))
//                    .requestTimeout(Duration.ofSeconds(stdioConfig.getRequestTimeout())).build();
//
//            McpSchema.InitializeResult initialize = mcpSyncClient.initialize();
//
//            log.info("tool stdio mcp initialize {}", initialize);
//            return mcpSyncClient;
//        }
//
//        throw new RuntimeException("tool mcp sse and stdio is null!");
//    }
}
