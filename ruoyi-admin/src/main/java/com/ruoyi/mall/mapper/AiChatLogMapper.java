package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.AiChatLog;

/**
 * AI对话日志 数据层
 *
 * @author ruoyi
 */
public interface AiChatLogMapper
{
    /**
     * 查询AI对话日志列表
     *
     * @param aiChatLog AI对话日志
     * @return AI对话日志集合
     */
    public List<AiChatLog> selectAiChatLogList(AiChatLog aiChatLog);

    /**
     * 查询AI对话日志详细
     *
     * @param id 日志id
     * @return AI对话日志
     */
    public AiChatLog selectAiChatLogById(Long id);

    /**
     * 新增AI对话日志
     *
     * @param aiChatLog AI对话日志
     * @return 结果
     */
    public int insertAiChatLog(AiChatLog aiChatLog);

    /**
     * 批量删除AI对话日志
     *
     * @param ids 需要删除的日志id数组
     * @return 结果
     */
    public int deleteAiChatLogByIds(Long[] ids);
}
