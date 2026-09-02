package com.ruoyi.mall.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI对话日志对象 ai_chat_log
 *
 * @author ruoyi
 */
public class AiChatLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志id（属性名 id 对应前端契约，列名 log_id） */
    private Long id;

    /** 会话id */
    private String sessionId;

    /** 用户问题 */
    private String question;

    /** AI回答 */
    private String answer;

    /** 意图（CONSULTATION咨询/ANALYTICS分析/OPERATION操作） */
    private String intent;

    /** Agent（本阶段与意图同值） */
    private String agent;

    /** 耗时（毫秒，Java侧测量） */
    private Integer latencyMs;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

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

    public String getAnswer()
    {
        return answer;
    }

    public void setAnswer(String answer)
    {
        this.answer = answer;
    }

    public String getIntent()
    {
        return intent;
    }

    public void setIntent(String intent)
    {
        this.intent = intent;
    }

    public String getAgent()
    {
        return agent;
    }

    public void setAgent(String agent)
    {
        this.agent = agent;
    }

    public Integer getLatencyMs()
    {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs)
    {
        this.latencyMs = latencyMs;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("sessionId", getSessionId())
            .append("question", getQuestion())
            .append("answer", getAnswer())
            .append("intent", getIntent())
            .append("agent", getAgent())
            .append("latencyMs", getLatencyMs())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
