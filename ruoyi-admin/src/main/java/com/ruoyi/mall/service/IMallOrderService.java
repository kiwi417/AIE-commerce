package com.ruoyi.mall.service;

import java.util.List;
import java.util.Map;

import com.ruoyi.mall.domain.OrderQueryParam;
import com.ruoyi.mall.domain.PortalOrder;

/**
 * 管理端订单Service接口（列表/详情/完成/取消）
 *
 * @author ruoyi
 */
public interface IMallOrderService
{
    /**
     * 订单列表（配合 PageHelper 分页）
     *
     * @param query 过滤条件
     * @return 订单集合
     */
    public List<PortalOrder> selectOrderList(OrderQueryParam query);

    /**
     * 订单详情（订单 + 明细，明细键名 orderItems 供管理端页面读取）
     *
     * @param orderId 订单id
     * @return {"order": 订单, "orderItems": 明细集合}
     */
    public Map<String, Object> selectOrderById(Long orderId);

    /**
     * 完成订单（仅 pending 可完成）
     *
     * @param orderId 订单id
     * @return 结果
     */
    public int completeOrder(Long orderId);

    /**
     * 取消订单（仅 pending 可取消，回补库存）
     *
     * @param orderId 订单id
     * @return 结果
     */
    public int cancelOrder(Long orderId);
}
