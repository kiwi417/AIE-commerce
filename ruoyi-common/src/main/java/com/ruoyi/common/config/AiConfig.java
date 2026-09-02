package com.ruoyi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI客服相关配置
 *
 * @author ruoyi
 */
@Component
@ConfigurationProperties(prefix = "ai")
public class AiConfig
{
    /** Python AI服务地址 */
    private String baseUrl;

    /** 连接超时（毫秒） */
    private int connectTimeoutMs;

    /** 读取/流式总超时（毫秒） */
    private int readTimeoutMs;

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMs()
    {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs)
    {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs()
    {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs)
    {
        this.readTimeoutMs = readTimeoutMs;
    }
}
