package com.lsp.trigger.http;

import com.alibaba.fastjson.JSON;
import com.lsp.api.IAgentService;
import com.lsp.api.dto.*;
import com.lsp.api.response.Response;
import com.lsp.domain.agent.model.entity.ChatCommandEntity;
import com.lsp.domain.agent.model.valobj.AiAgentConfigTableVO;
import com.lsp.domain.agent.service.IChatService;
import com.lsp.domain.credit.model.entity.CreditAccountEntity;
import com.lsp.domain.credit.model.entity.CreditUseEntity;
import com.lsp.domain.credit.service.ICreditService;
import com.lsp.types.enums.ResponseCode;
import com.lsp.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/")
@CrossOrigin(origins = "*")
public class AgentServiceController implements IAgentService {


    @Resource
    private IChatService chatService;

    @Resource
    private ICreditService creditService;


    @RequestMapping(value = "query_ai_agent_config_list", method = RequestMethod.GET)
    @Override
    public Response<List<AiAgentConfigResponseDTO>> queryAiAgentConfigList() {
        try {
            log.info("查询智能体配置列表");

            List<AiAgentConfigTableVO.Agent> agentConfigs = chatService.queryAiAgentConfigList();

            List<AiAgentConfigResponseDTO> responseDTOS = agentConfigs.stream().map(agentConfig -> {
                AiAgentConfigResponseDTO responseDTO = new AiAgentConfigResponseDTO();
                responseDTO.setAgentId(agentConfig.getAgentId());
                responseDTO.setAgentName(agentConfig.getAgentName());
                responseDTO.setAgentDesc(agentConfig.getAgentDesc());
                return responseDTO;
            }).collect(Collectors.toList());

            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTOS)
                    .build();

        } catch (AppException e) {
            log.error("查询智能体配置列表异常", e);
            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("查询智能体配置列表失败", e);
            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "create_session", method = {RequestMethod.POST})
    @Override
    public Response<CreateSessionResponseDTO> createSession(@RequestBody CreateSessionRequestDTO requestDTO) {
        try {
            log.info("创建会话 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId());
            String sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());

            CreateSessionResponseDTO responseDTO = new CreateSessionResponseDTO();
            responseDTO.setSessionId(sessionId);

            return Response.<CreateSessionResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (AppException e) {
            log.error("查询智能体配置列表异常", e);
            return Response.<CreateSessionResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("创建会话失败 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId(), e);
            return Response.<CreateSessionResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "chat", method = RequestMethod.POST)
    @Override
    public Response<ChatResponseDTO> chat(@RequestBody ChatRequestDTO requestDTO) {
        try {
            log.info("智能体对话 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId());
            if (!hasAvailableCredits(requestDTO.getUserId())) {
                return buildInsufficientCreditsResponse();
            }

            String sessionId = requestDTO.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());
            }

            String requestId = resolveRequestId(requestDTO.getRequestId());
            long startedAt = System.currentTimeMillis();
            ChatRunResult chatRunResult = runChatWithSessionRecovery(
                    requestDTO.getAgentId(),
                    requestDTO.getUserId(),
                    sessionId,
                    requestDTO.getMessage());
            sessionId = chatRunResult.sessionId();
            List<String> messages = chatRunResult.messages();
            long durationMs = System.currentTimeMillis() - startedAt;

            ChatResponseDTO responseDTO = new ChatResponseDTO();
            String fallbackResult = findLastNonBlankMessage(messages);
            try {
                // 把智能体返回的最后一条消息，尽量解析成 ChatResponseDTO，方便前端判断是普通文本回复，还是 draw.io 图表数据
                String result = findLastNonBlankMessage(messages);
                log.info("智能体返回消息数量:{} 最终内容长度:{}", null == messages ? 0 : messages.size(), result.length());
                ChatResponseDTO parsed = JSON.parseObject(result, ChatResponseDTO.class);
                if (null != parsed) {
                    responseDTO = parsed;
                    // 如果解析后的对象 type 为空，则默认为 user
                    if (null == responseDTO.getType()) {
                        responseDTO.setType("user");
                    }
                } else {
                    responseDTO.setType("user");
                    responseDTO.setContent(fallbackResult);
                }
            } catch (Exception e) {
                responseDTO.setType("user");
                responseDTO.setContent(fallbackResult);
            }

            fillCreditUsage(responseDTO, requestDTO.getUserId(), requestId, requestDTO.getAgentId(), sessionId, durationMs, "chat");

            return Response.<ChatResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (AppException e) {
            log.error("智能体对话异常", e);
            return Response.<ChatResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("智能体对话失败 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId(), e);
            return Response.<ChatResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "chat_stream", method = RequestMethod.POST, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Override
    public SseEmitter chatStream(@RequestBody ChatRequestDTO requestDTO) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        try {
            String sessionId = requestDTO.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());
            }

            log.info("流式对话 agentId:{} userId:{} sessionId:{} message:{}", requestDTO.getAgentId(), requestDTO.getUserId(), sessionId, requestDTO.getMessage());
            chatService.handleMessageStream(requestDTO.getAgentId(), requestDTO.getUserId(), sessionId, requestDTO.getMessage())
                    .subscribe(
                            event -> {
                                try {
                                    String content = event.stringifyContent();
                                    if (content != null && !content.isBlank()) {
                                        emitter.send(SseEmitter.event().data(content));
                                    }
                                } catch (Exception e) {
                                    log.error("流式对话发送失败", e);
                                    emitter.completeWithError(e);
                                }
                            },
                            emitter::completeWithError,
                            emitter::complete
                    );
        } catch (Exception e) {
            log.error("流式对话失败", e);
            emitter.completeWithError(e);
        }
        return emitter;
    }


    @RequestMapping(value = "analyze_diagram_image", method = RequestMethod.POST)
    public Response<ChatResponseDTO> analyzeDiagramImage(@RequestBody AnalyzeDiagramImageRequestDTO requestDTO) {

        try {
            if (!hasAvailableCredits(requestDTO.getUserId())) {
                return buildInsufficientCreditsResponse();
            }

            String sessionId = requestDTO.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());
            }

            ImageData imageData = parseImageDataUrl(requestDTO.getImageDataUrl());

            ChatCommandEntity chatCommandEntity = ChatCommandEntity.builder()
                    .agentId(requestDTO.getAgentId())
                    .userId(requestDTO.getUserId())
                    .sessionId(sessionId)
                    .texts(List.of(new ChatCommandEntity.Content.Text(requestDTO.getMessage())))
                    .files(List.of())
                    .inlineDatas(List.of(
                            new ChatCommandEntity.Content.InlineData(
                                    imageData.bytes(),
                                    imageData.mimeType()
                            )
                    ))
                    .build();

            log.info("inlineDatas size:{}", chatCommandEntity.getInlineDatas().size());


            String requestId = resolveRequestId(requestDTO.getRequestId());
            long startedAt = System.currentTimeMillis();
            ChatRunResult chatRunResult = runChatCommandWithSessionRecovery(chatCommandEntity);
            sessionId = chatRunResult.sessionId();
            List<String> messages = chatRunResult.messages();
            long durationMs = System.currentTimeMillis() - startedAt;
            ChatResponseDTO responseDTO = new ChatResponseDTO();
            String fallbackResult = findLastNonBlankMessage(messages);
            try {
                // 把智能体返回的最后一条消息，尽量解析成 ChatResponseDTO，方便前端判断是普通文本回复，还是 draw.io 图表数据
                String result = findLastNonBlankMessage(messages);
                log.info("智能体返回消息数量:{} 最终内容长度:{}", null == messages ? 0 : messages.size(), result.length());
                ChatResponseDTO parsed = JSON.parseObject(result, ChatResponseDTO.class);
                if (null != parsed) {
                    responseDTO = parsed;
                    // 如果解析后的对象 type 为空，则默认为 user
                    if (null == responseDTO.getType()) {
                        responseDTO.setType("user");
                    }
                } else {
                    responseDTO.setType("user");
                    responseDTO.setContent(fallbackResult);
                }
            } catch (Exception e) {
                responseDTO.setType("user");
                responseDTO.setContent(fallbackResult);
            }

            fillCreditUsage(responseDTO, requestDTO.getUserId(), requestId, requestDTO.getAgentId(), sessionId, durationMs, "analyze_diagram_image");

            return Response.<ChatResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();

        } catch (AppException e) {
            return Response.<ChatResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();

        } catch (Exception e) {
            return Response.<ChatResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }


    private ImageData parseImageDataUrl(String imageDataUrl) {
        int mimeStart = imageDataUrl.indexOf(':');
        int mimeEnd = imageDataUrl.indexOf(';');
        int dataStart = imageDataUrl.indexOf(',');

        if (mimeStart < 0 || mimeEnd < 0 || dataStart < 0 || dataStart <= mimeEnd) {
            throw new IllegalArgumentException("图片格式不正确，请上传 Base64 Data URL 图片。");
        }

        String mimeType = imageDataUrl.substring(mimeStart + 1, mimeEnd);
        String base64 = imageDataUrl.substring(dataStart + 1);
        byte[] bytes = Base64.getDecoder().decode(base64);
        return new ImageData(bytes, mimeType);
    }

    private String findLastNonBlankMessage(List<String> messages) {
        if (null == messages || messages.isEmpty()) {
            return "";
        }

        for (int index = messages.size() - 1; index >= 0; index--) {
            String message = messages.get(index);
            if (null != message && !message.isBlank()) {
                return message;
            }
        }

        return "";
    }

    private ChatRunResult runChatWithSessionRecovery(String agentId, String userId, String sessionId, String message) {
        try {
            return new ChatRunResult(sessionId, chatService.handleMessage(agentId, userId, sessionId, message));
        } catch (IllegalArgumentException e) {
            if (!isSessionNotFound(e)) {
                throw e;
            }

            String freshSessionId = chatService.recreateSession(agentId, userId);
            log.warn("ADK session missing, recreated session. agentId:{} userId:{} oldSessionId:{} newSessionId:{}",
                    agentId, userId, sessionId, freshSessionId);
            return new ChatRunResult(freshSessionId, chatService.handleMessage(agentId, userId, freshSessionId, message));
        }
    }

    private ChatRunResult runChatCommandWithSessionRecovery(ChatCommandEntity chatCommandEntity) {
        try {
            return new ChatRunResult(chatCommandEntity.getSessionId(), chatService.handleMessage(chatCommandEntity));
        } catch (IllegalArgumentException e) {
            if (!isSessionNotFound(e)) {
                throw e;
            }

            String freshSessionId = chatService.recreateSession(chatCommandEntity.getAgentId(), chatCommandEntity.getUserId());
            log.warn("ADK session missing, recreated image session. agentId:{} userId:{} oldSessionId:{} newSessionId:{}",
                    chatCommandEntity.getAgentId(), chatCommandEntity.getUserId(), chatCommandEntity.getSessionId(), freshSessionId);
            chatCommandEntity.setSessionId(freshSessionId);
            return new ChatRunResult(freshSessionId, chatService.handleMessage(chatCommandEntity));
        }
    }

    private boolean isSessionNotFound(Exception e) {
        return null != e.getMessage() && e.getMessage().contains("Session not found");
    }

    private boolean hasAvailableCredits(String userId) {
        if (null == userId || userId.isBlank()) {
            return false;
        }

        CreditAccountEntity creditAccountEntity = creditService.queryCreditAccount(userId);
        return null != creditAccountEntity
                && null != creditAccountEntity.getAvailableCredits()
                && creditAccountEntity.getAvailableCredits().compareTo(BigDecimal.ZERO) > 0;
    }

    private Response<ChatResponseDTO> buildInsufficientCreditsResponse() {
        return Response.<ChatResponseDTO>builder()
                .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                .info("额度不足，请购买额度后继续使用")
                .build();
    }

    private String resolveRequestId(String requestId) {
        if (null != requestId && !requestId.isBlank()) {
            return requestId.trim();
        }

        return UUID.randomUUID().toString().replace("-", "");
    }

    private void fillCreditUsage(ChatResponseDTO responseDTO, String userId, String requestId, String agentId,
                                 String sessionId, long durationMs, String remark) {
        CreditUseEntity creditUseEntity = creditService.consumeCredits(userId, requestId, agentId, sessionId, durationMs, remark);
        responseDTO.setSessionId(sessionId);
        responseDTO.setRequestId(requestId);
        responseDTO.setDurationMs(durationMs);
        responseDTO.setCostCredits(toInt(creditUseEntity.getUsedCredits()));
        responseDTO.setRemainingCredits(toInt(creditUseEntity.getRemainingCredits()));
    }

    private int toInt(BigDecimal value) {
        if (null == value) {
            return 0;
        }

        return value.intValue();
    }

    private record ChatRunResult(String sessionId, List<String> messages) {
    }

    private record ImageData(byte[] bytes, String mimeType) {
    }

}
