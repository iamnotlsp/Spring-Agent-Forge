package com.lsp.domain.agent.service;

import com.google.adk.events.Event;
import com.lsp.domain.agent.model.entity.ChatCommandEntity;
import com.lsp.domain.agent.model.valobj.AiAgentConfigTableVO;
import io.reactivex.rxjava3.core.Flowable;

import java.util.List;

/**
 * @description 对话服务接口
 * @author 林善鹏
 * @date 2026-05-21 15:01
 */
public interface IChatService {

    /**
     * 查询当前系统中已经配置的智能体基础信息列表
     */
    List<AiAgentConfigTableVO.Agent> queryAiAgentConfigList();

    /**
     * 为指定用户创建智能体会话。如果该用户已存在会话，则复用已有 sessionId
     */
    String createSession(String agentId, String userId);

    /**
     * 发送纯文本消息并执行智能体；如果用户没有会话，则自动创建会话。
     */
    List<String> handleMessage(String agentId, String userId, String message);

    /**
     * 在指定 session 中发送纯文本消息，执行智能体并返回本次执行产生的所有文本事件结果。
     */
    List<String> handleMessage(String agentId, String userId, String sessionId, String message);

    /**
     * 在指定 session 中发送纯文本消息，并以事件流形式返回智能体执行结果，适用于流式响应场景。
     */
    Flowable<Event> handleMessageStream(String agentId, String userId, String sessionId, String message);

    /**
     * 处理结构化聊天命令，支持文本、文件 URI、内联二进制数据等多种输入内容，并返回智能体执行结果。
     */
    List<String> handleMessage(ChatCommandEntity chatCommandEntity);

    /**
     * 以事件流形式处理结构化聊天命令，支持文本、文件 URI、内联二进制数据等多种输入内容。
     */
    Flowable<Event> handleMessageStream(ChatCommandEntity chatCommandEntity);

}
