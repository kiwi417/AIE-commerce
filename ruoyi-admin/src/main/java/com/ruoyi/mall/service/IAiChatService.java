package com.ruoyi.mall.service;

import java.util.Map;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * AI客服Service接口
 *
 * @author ruoyi
 */
public interface IAiChatService
{
    /**
     * 创建会话
     *
     * @return 会话id
     */
    public String createSession();

    /**
     * 非流式对话
     *
     * @param sessionId 会话id
     * @param question 用户问题
     * @return 应答数据（answer/toolCalled/products/categories）
     */
    public Map<String, Object> chat(String sessionId, String question);

    /**
     * 流式对话：转发 Python AI 服务的裸文本流（前端逐字符渲染，非 SSE 格式）
     *
     * @param sessionId 会话id
     * @param message 用户消息
     * @return 流式响应体
     */
    public StreamingResponseBody streamChat(String sessionId, String message);

    /**
     * 会话工具卡片数据（骨架阶段为空结构）
     *
     * @param sessionId 会话id
     * @return 工具数据
     */
    public Map<String, Object> toolData(String sessionId);

    /**
     * 清空会话（清空对话按钮调用）：删除 Redis 中该会话的历史消息与检索上下文。
     * 会话 id 保持不变——购物车也挂在同一 id 下，换 id 会导致购物车"丢失"。
     *
     * @param sessionId 会话id
     */
    public void clearSession(String sessionId);
}
