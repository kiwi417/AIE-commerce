package com.ruoyi.mall.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.config.AiConfig;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.mall.domain.AiChatLog;
import com.ruoyi.mall.mapper.AiChatLogMapper;
import com.ruoyi.mall.service.IAiChatService;
import com.ruoyi.mall.service.IPortalCartService;

/**
 * AI客服Service业务层处理：转发 Python AI 服务，流式应答与对话日志落库
 *
 * @author ruoyi
 */
@Service
public class AiChatServiceImpl implements IAiChatService
{
    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    /** 流式失败时的纯文本兜底文案（前端会把响应体逐字当答案展示，不能是 JSON） */
    private static final String STREAM_ERROR_TEXT = "抱歉，AI服务暂时不可用，请稍后重试。";

    /** 流式中断提示 */
    private static final String STREAM_BROKEN_TEXT = "\n\n（连接中断，回答可能不完整）";

    private final AiConfig aiConfig;

    private final AiChatLogMapper aiChatLogMapper;

    private final IPortalCartService portalCartService;

    private final RedisCache redisCache;

    private final HttpClient httpClient;

    public AiChatServiceImpl(AiConfig aiConfig, AiChatLogMapper aiChatLogMapper, IPortalCartService portalCartService,
                             RedisCache redisCache)
    {
        this.aiConfig = aiConfig;
        this.aiChatLogMapper = aiChatLogMapper;
        this.portalCartService = portalCartService;
        this.redisCache = redisCache;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(aiConfig.getConnectTimeoutMs()))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * 创建会话：UUID 去横线（Python 端对任意 sessionId 惰性建会话）
     */
    @Override
    public String createSession()
    {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 非流式对话
     */
    @Override
    public Map<String, Object> chat(String sessionId, String question)
    {
        long start = System.currentTimeMillis();
        JSONObject body = new JSONObject();
        body.put("session_id", sessionId);
        body.put("question", question);
        try
        {
            HttpResponse<InputStream> response = postStream(aiConfig.getBaseUrl() + "/ai/chat", body.toString());
            JSONObject data = JSON.parseObject(new String(response.body().readAllBytes(), StandardCharsets.UTF_8));
            String answer = data.getString("answer");
            String intent = data.getString("intent");
            String agent = data.getString("agent");
            saveChatLog(sessionId, question, answer, intent, agent, System.currentTimeMillis() - start);

            Map<String, Object> result = new HashMap<>();
            result.put("answer", answer);
            result.put("toolCalled", Boolean.TRUE.equals(data.getBoolean("toolCalled")));
            result.put("products", data.getJSONArray("products") == null ? new JSONArray() : data.getJSONArray("products"));
            result.put("categories", data.getJSONArray("categories") == null ? new JSONArray() : data.getJSONArray("categories"));
            return result;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new ServiceException("AI服务调用被中断，请稍后重试");
        }
        catch (Exception e)
        {
            log.error("调用AI服务失败: {}", e.getMessage());
            throw new ServiceException("AI服务不可用，请稍后重试");
        }
    }

    /**
     * 流式对话：同步建立上游连接并读取元数据响应头后，返回字节原样转发（不做逐块解码，
     * 避免中文多字节序列被 chunk 边界切断），流结束后无论成败写对话日志。
     */
    @Override
    public StreamingResponseBody streamChat(String sessionId, String message)
    {
        JSONObject body = new JSONObject();
        body.put("session_id", sessionId);
        body.put("message", message);

        String intent;
        String agent;
        HttpResponse<InputStream> response;
        try
        {
            // 阶段A：响应头在流开始前固定，intent/agent 必须先拿到
            response = postStream(aiConfig.getBaseUrl() + "/ai/chat/stream", body.toString());
            intent = response.headers().firstValue("X-Intent").orElse("CONSULTATION");
            agent = response.headers().firstValue("X-Agent").orElse(intent);
        }
        catch (Exception e)
        {
            log.error("连接AI流式服务失败: {}", e.getMessage());
            // Python 不可用时仍返回纯文本兜底（200 + 文案），前端会将其作为答案展示
            return out -> out.write(STREAM_ERROR_TEXT.getBytes(StandardCharsets.UTF_8));
        }

        long start = System.currentTimeMillis();
        // 阶段B：交给 Spring 执行的字节转发
        return out ->
        {
            ByteArrayOutputStream acc = new ByteArrayOutputStream();
            boolean broken = false;
            try (InputStream in = response.body())
            {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1)
                {
                    out.write(buf, 0, n);
                    out.flush();
                    acc.write(buf, 0, n);
                }
            }
            catch (IOException e)
            {
                broken = true;
            }
            finally
            {
                try
                {
                    if (acc.size() == 0)
                    {
                        out.write(STREAM_ERROR_TEXT.getBytes(StandardCharsets.UTF_8));
                    }
                    else if (broken)
                    {
                        out.write(STREAM_BROKEN_TEXT.getBytes(StandardCharsets.UTF_8));
                    }
                    out.flush();
                }
                catch (IOException ignore)
                {
                }
                // 无论成功/中断都写日志（部分回答也落库，便于排查）
                saveChatLog(sessionId, message, acc.toString(StandardCharsets.UTF_8), intent, agent,
                        System.currentTimeMillis() - start);
            }
        };
    }

    /**
     * 工具卡片数据：转发 Python，失败返回空结构保证前端轮询不报错
     */
    @Override
    public Map<String, Object> toolData(String sessionId)
    {
        Map<String, Object> result;
        try
        {
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(aiConfig.getBaseUrl() + "/ai/chat/tool-data?session_id=" + sessionId))
                    .timeout(Duration.ofMillis(aiConfig.getReadTimeoutMs()))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            JSONObject data = JSON.parseObject(new String(response.body().readAllBytes(), StandardCharsets.UTF_8));
            result = toolDataFrom(data);
        }
        catch (Exception e)
        {
            log.warn("获取AI工具数据失败: {}", e.getMessage());
            result = emptyToolData();
        }
        // 无论 Python 是否可用，都合并 Java Redis 购物车（前端聊天面板可展示购物车卡片）
        mergeCartItems(result, sessionId);
        return result;
    }

    /**
     * 清空会话：删除 Python 端写入的会话历史与检索上下文（ai:session:/ai:ctx:）。
     * 与 Python 共用同一 Redis（db 一致），Java 直接删 key 即可，无需转发 Python。
     * 注意：不清 mall:cart:——购物车与聊天共用会话 id，清空对话不能丢购物车。
     */
    @Override
    public void clearSession(String sessionId)
    {
        try
        {
            redisCache.deleteObject("ai:session:" + sessionId);
            redisCache.deleteObject("ai:ctx:" + sessionId);
        }
        catch (Exception e)
        {
            log.warn("清空会话失败: {}", e.getMessage());
        }
    }

    /**
     * 合并购物车：失败不影响主结构（购物车卡片只是附加信息）
     */
    private void mergeCartItems(Map<String, Object> result, String sessionId)
    {
        try
        {
            result.put("cartItems", portalCartService.listItems(sessionId));
        }
        catch (Exception e)
        {
            log.warn("合并购物车失败: {}", e.getMessage());
        }
    }

    private Map<String, Object> toolDataFrom(JSONObject data)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("products", data.getJSONArray("products") == null ? new JSONArray() : data.getJSONArray("products"));
        result.put("categories", data.getJSONArray("categories") == null ? new JSONArray() : data.getJSONArray("categories"));
        result.put("cartItems", data.getJSONArray("cartItems") == null ? new JSONArray() : data.getJSONArray("cartItems"));
        result.put("order", data.get("order"));
        result.put("orderItems", data.getJSONArray("orderItems") == null ? new JSONArray() : data.getJSONArray("orderItems"));
        result.put("toolCalled", Boolean.TRUE.equals(data.getBoolean("toolCalled")));
        return result;
    }

    private Map<String, Object> emptyToolData()
    {
        Map<String, Object> result = new HashMap<>();
        result.put("products", new JSONArray());
        result.put("categories", new JSONArray());
        result.put("cartItems", new JSONArray());
        result.put("order", null);
        result.put("orderItems", new JSONArray());
        result.put("toolCalled", false);
        return result;
    }

    /**
     * 写对话日志：门户匿名请求下 SecurityContext 无 LoginUser，不能调 SecurityUtils（会抛异常），
     * 必须判空；日志落库失败不影响对话链路。
     */
    private void saveChatLog(String sessionId, String question, String answer, String intent, String agent, long latencyMs)
    {
        try
        {
            AiChatLog chatLog = new AiChatLog();
            chatLog.setSessionId(sessionId);
            chatLog.setQuestion(question);
            chatLog.setAnswer(answer);
            chatLog.setIntent(intent);
            chatLog.setAgent(agent);
            chatLog.setLatencyMs((int) latencyMs);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof LoginUser)
            {
                LoginUser loginUser = (LoginUser) auth.getPrincipal();
                chatLog.setCreateBy(String.valueOf(loginUser.getUserId()));
            }
            chatLog.setCreateTime(new Date());
            aiChatLogMapper.insertAiChatLog(chatLog);
        }
        catch (Exception e)
        {
            log.warn("AI对话日志写入失败: {}", e.getMessage());
        }
    }

    /**
     * POST JSON 并返回 InputStream 响应体（流式读取）
     */
    private HttpResponse<InputStream> postStream(String url, String json) throws IOException, InterruptedException
    {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(aiConfig.getReadTimeoutMs()))
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }
}
