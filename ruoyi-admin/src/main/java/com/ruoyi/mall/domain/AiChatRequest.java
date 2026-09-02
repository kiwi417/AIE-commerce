package com.ruoyi.mall.domain;

/**
 * AI对话请求（非流式，对应前端 body {sessionId, question}）
 *
 * @author ruoyi
 */
public class AiChatRequest
{
    /** 会话id */
    private String sessionId;

    /** 用户问题 */
    private String question;

    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    public String getQuestion()
    {
        return question;
    }

    public void setQuestion(String question)
    {
        this.question = question;
    }
}
