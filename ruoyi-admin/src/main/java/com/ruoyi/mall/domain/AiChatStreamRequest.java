package com.ruoyi.mall.domain;

/**
 * AI对话请求（流式，对应前端 body {sessionId, message}）
 *
 * @author ruoyi
 */
public class AiChatStreamRequest
{
    /** 会话id */
    private String sessionId;

    /** 用户消息 */
    private String message;

    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}
