package com.ruoyi.mall.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.mall.domain.AiChatLog;
import com.ruoyi.mall.service.IAiChatLogService;

/**
 * AI对话日志管理（后台管理端，需登录）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/ai/chatlog")
public class AiChatLogController extends BaseController
{
    @Autowired
    private IAiChatLogService aiChatLogService;

    /**
     * 查询AI对话日志列表
     */
    @GetMapping("/list")
    public TableDataInfo list(AiChatLog aiChatLog)
    {
        startPage();
        return getDataTable(aiChatLogService.selectAiChatLogList(aiChatLog));
    }

    /**
     * 获取AI对话日志详细信息
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(aiChatLogService.selectAiChatLogById(id));
    }

    /**
     * 删除AI对话日志
     */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(aiChatLogService.deleteAiChatLogByIds(ids));
    }
}
