package com.ruoyi.mall.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.alibaba.fastjson2.JSONArray;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.mall.domain.AiChatRequest;
import com.ruoyi.mall.domain.AiChatStreamRequest;
import com.ruoyi.mall.service.IAiChatService;

/**
 * AI智能客服接口（门户端，免登录）
 *
 * @author ruoyi
 */
@Anonymous
@RestController
@RequestMapping("/ai/chat")
public class AiChatController
{
    @Autowired
    private IAiChatService aiChatService;

    /**
     * 创建会话
     */
    @PostMapping("/session")
    public AjaxResult createSession()
    {
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", aiChatService.createSession());
        return AjaxResult.success(data);
    }

    /**
     * 非流式对话（门户chat页使用）
     */
    @PostMapping
    public AjaxResult chat(@RequestBody AiChatRequest request)
    {
        return AjaxResult.success(aiChatService.chat(request.getSessionId(), request.getQuestion()));
    }

    /**
     * 流式对话：注意！前端 AiChatBox 契约要求纯文本流，本接口是全项目唯一
     * 不返回 AjaxResult JSON 的端点，请勿"修正"为统一返回结构。
     */
    @PostMapping("/stream")
    public ResponseEntity<StreamingResponseBody> stream(@RequestBody AiChatStreamRequest request)
    {
        return ResponseEntity.ok()
                .header("Content-Type", "text/plain;charset=UTF-8")
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(aiChatService.streamChat(request.getSessionId(), request.getMessage()));
    }

    /**
     * 工具卡片数据（流结束后前端轮询；骨架阶段为空结构）
     */
    @GetMapping("/tool-data")
    public AjaxResult toolData(@RequestParam("sessionId") String sessionId)
    {
        return AjaxResult.success(aiChatService.toolData(sessionId));
    }

    /**
     * 清空会话（清空对话按钮调用）：只清聊天历史与检索上下文，不清购物车
     */
    @GetMapping("/clear-session")
    public AjaxResult clearSession(@RequestParam("sessionId") String sessionId)
    {
        aiChatService.clearSession(sessionId);
        return AjaxResult.success();
    }

    /**
     * 图片对话：DeepSeek 无视觉能力，本阶段为占位实现，接入视觉模型后改造
     */
    @PostMapping("/image")
    public AjaxResult image(@RequestParam("file") MultipartFile file,
                            @RequestParam(value = "message", required = false, defaultValue = "") String message,
                            @RequestParam(value = "sessionId", required = false, defaultValue = "") String sessionId)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("answer", "图片识别功能正在建设中，暂时无法识别图片内容，请先用文字向我提问～");
        data.put("products", new JSONArray());
        data.put("imageDescription", "已收到图片：" + file.getOriginalFilename() + "（" + file.getSize() + " 字节）");
        return AjaxResult.success(data);
    }
}
