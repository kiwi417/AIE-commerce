package com.ruoyi.mall.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.mall.domain.AiChatLog;
import com.ruoyi.mall.mapper.AiChatLogMapper;
import com.ruoyi.mall.service.IAiChatLogService;

/**
 * AI对话日志Service业务层处理
 *
 * @author ruoyi
 */
@Service
public class AiChatLogServiceImpl implements IAiChatLogService
{
    @Autowired
    private AiChatLogMapper aiChatLogMapper;

    /**
     * 查询AI对话日志列表
     *
     * @param aiChatLog AI对话日志
     * @return AI对话日志集合
     */
    @Override
    public List<AiChatLog> selectAiChatLogList(AiChatLog aiChatLog)
    {
        return aiChatLogMapper.selectAiChatLogList(aiChatLog);
    }

    /**
     * 查询AI对话日志详细
     *
     * @param id 日志id
     * @return AI对话日志
     */
    @Override
    public AiChatLog selectAiChatLogById(Long id)
    {
        return aiChatLogMapper.selectAiChatLogById(id);
    }

    /**
     * 新增AI对话日志
     *
     * @param aiChatLog AI对话日志
     * @return 结果
     */
    @Override
    public int insertAiChatLog(AiChatLog aiChatLog)
    {
        return aiChatLogMapper.insertAiChatLog(aiChatLog);
    }

    /**
     * 批量删除AI对话日志
     *
     * @param ids 需要删除的日志id数组
     * @return 结果
     */
    @Override
    public int deleteAiChatLogByIds(Long[] ids)
    {
        return aiChatLogMapper.deleteAiChatLogByIds(ids);
    }
}
