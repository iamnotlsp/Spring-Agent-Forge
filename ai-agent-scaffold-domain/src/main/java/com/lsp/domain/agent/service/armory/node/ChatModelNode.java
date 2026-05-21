package com.lsp.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsp.domain.agent.model.entity.ArmoryCommandEntity;
import com.lsp.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.lsp.domain.agent.model.valobj.AiAgentRegisterVO;
import com.lsp.domain.agent.service.armory.AbstractArmorySupport;
import com.lsp.domain.agent.service.armory.factory.DefaultArmoryFactory;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChatModelNode extends AbstractArmorySupport {

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - ChatModelNode - 构建对话模型配置及加入MCP工具至模型配置");

        OpenAiApi openAiApi = dynamicContext.getOpenAiApi();

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        // 得到对话模型配置
        AiAgentConfigTableVO.Module.ChatModel chatModelConfig = aiAgentConfigTableVO.getModule().getChatModel();
        // 建立一个列表，用来存放所有 MCP 客户端
        List<McpSyncClient> mcpSyncClients = new ArrayList<>();
        List<AiAgentConfigTableVO.Module.ChatModel.ToolMcp> toolMcpList = chatModelConfig.getToolMcpList();
        for (AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp : toolMcpList) {
            // 判断工具的MCP配置是SSE还是Stdio，并创建相应的MCP客户端实例，最后将其添加到列表中
            mcpSyncClients.add(createMcpSyncClient(toolMcp));
        }

        // 构建一个ChatModel（OpenAI 兼容模型）并把LLM api连接 和 MCP工具能力一起挂载进去（使其既能对话又能调用外部工具）
        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatModelConfig.getModel())
                        .toolCallbacks(SyncMcpToolCallbackProvider.builder()
                                .mcpClients(mcpSyncClients).build()
                                .getToolCallbacks())
                        .build())
                .build();

        // 将构建好的 ChatModel 放入动态上下文中，以便后续节点使用
        dynamicContext.setChatModel(chatModel);

        return router(requestParameter, dynamicContext);

    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }
    /**
     * @description  如果配置了 SSE，就连接远程 MCP；如果配置了 Stdio，就启动本地 MCP；
     * @author 林善鹏
     * @date 2026-05-21 16:38
     */
    private McpSyncClient createMcpSyncClient(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception {

        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.SSEServerParameters sseConfig = toolMcp.getSse();
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters stdioConfig = toolMcp.getStdio();

        if (null != sseConfig) {
            // http://appbuilder.baidu.com/v2/ai_search/mcp/sse?api_key=bce-v3/ALTAK-JFZXXLpfxhAutDQvJ32Ei/4492c1879b8c2f0df4612ef5b4a52df1c1fba9f7

            String originalBaseUri = sseConfig.getBaseUri();
            String baseUri = originalBaseUri;
            String sseEndpoint = sseConfig.getSseEndpoint();

            if (StringUtils.isBlank(sseEndpoint)) {
                URL url = new URL(originalBaseUri);

                String protocol = url.getProtocol();
                String host = url.getHost();
                int port = url.getPort();

                String baseUrl = port == -1 ? protocol + "://" + host : protocol + "://" + host + ":" + port;

                int index = originalBaseUri.indexOf(baseUrl);
                if (index != -1) {
                    sseEndpoint = originalBaseUri.substring(index + baseUrl.length());
                }

                baseUri = baseUrl;
            }

            sseEndpoint = StringUtils.isBlank(sseEndpoint) ? "/sse" : sseEndpoint;

            HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
                    .builder(baseUri)
                    .sseEndpoint(sseEndpoint)
                    .build();

            McpSyncClient mcpSyncClient = McpClient
                    .sync(sseClientTransport)
                    .requestTimeout(Duration.ofMillis(sseConfig.getRequestTimeout())).build();
            McpSchema.InitializeResult initialize = mcpSyncClient.initialize();

            log.info("tool sse mcp initialize {}", initialize);

            return mcpSyncClient;
        }

        if (null != stdioConfig) {
            AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters.ServerParameters serverParameters = stdioConfig.getServerParameters();

            ServerParameters stdioParams = ServerParameters.builder(serverParameters.getCommand())
                    .args(serverParameters.getArgs())
                    .env(serverParameters.getEnv())
                    .build();

            McpSyncClient mcpSyncClient = McpClient.sync(new StdioClientTransport(stdioParams, new JacksonMcpJsonMapper(new ObjectMapper())))
                    .requestTimeout(Duration.ofSeconds(stdioConfig.getRequestTimeout())).build();

            McpSchema.InitializeResult initialize = mcpSyncClient.initialize();

            log.info("tool stdio mcp initialize {}", initialize);
            return mcpSyncClient;
        }

        throw new RuntimeException("tool mcp sse and stdio is null!");
    }
}
