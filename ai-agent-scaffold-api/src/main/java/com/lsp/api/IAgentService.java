package com.lsp.api;

import com.lsp.api.dto.*;
import com.lsp.api.response.Response;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * @description 对外接口定义
 * @author 林善鹏
 * @date 2026-05-29 16:31
 */

public interface IAgentService {

    Response<List<AiAgentConfigResponseDTO>> queryAiAgentConfigList();

    Response<CreateSessionResponseDTO> createSession(CreateSessionRequestDTO requestDTO);

    Response<ChatResponseDTO> chat(ChatRequestDTO requestDTO);

    SseEmitter chatStream(ChatRequestDTO requestDTO);

}
