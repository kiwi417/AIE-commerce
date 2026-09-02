package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.CartItem;

/**
 * 门户购物车Service接口（会话购物车，Redis 存储商品快照，无 MySQL 表）
 *
 * @author ruoyi
 */
public interface IPortalCartService
{
    /**
     * 加入购物车（已存在则累加数量）
     *
     * @param sessionId 会话id
     * @param productId 商品id
     * @param quantity 数量（空视为1）
     */
    public void addItem(String sessionId, Long productId, Integer quantity);

    /**
     * 查询购物车条目列表（未知会话返回空列表）
     *
     * @param sessionId 会话id
     * @return 条目集合
     */
    public List<CartItem> listItems(String sessionId);

    /**
     * 更新数量（数量<=0 视为删除）
     *
     * @param sessionId 会话id
     * @param productId 商品id
     * @param quantity 数量
     */
    public void updateItem(String sessionId, Long productId, Integer quantity);

    /**
     * 移除条目
     *
     * @param sessionId 会话id
     * @param productId 商品id
     */
    public void removeItem(String sessionId, Long productId);

    /**
     * 清空购物车
     *
     * @param sessionId 会话id
     */
    public void clear(String sessionId);
}
