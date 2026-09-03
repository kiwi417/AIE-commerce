package com.ruoyi.mall.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.mall.domain.OrderQueryParam;
import com.ruoyi.mall.domain.PortalOrder;
import com.ruoyi.mall.domain.PortalOrderItem;
import com.ruoyi.mall.mapper.PortalOrderMapper;
import com.ruoyi.mall.service.IMallOrderService;

/**
 * 管理端订单Service业务层处理：状态流转 pending→completed / pending→cancelled，
 * 取消订单回补库存，重复或非法流转抛业务异常。
 *
 * @author ruoyi
 */
@Service
public class MallOrderServiceImpl implements IMallOrderService
{
    private final PortalOrderMapper portalOrderMapper;

    public MallOrderServiceImpl(PortalOrderMapper portalOrderMapper)
    {
        this.portalOrderMapper = portalOrderMapper;
    }

    @Override
    public List<PortalOrder> selectOrderList(OrderQueryParam query)
    {
        return portalOrderMapper.selectOrderList(query);
    }

    @Override
    public Map<String, Object> selectOrderById(Long orderId)
    {
        PortalOrder order = portalOrderMapper.selectOrderById(orderId);
        if (order == null)
        {
            throw new ServiceException("订单不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("orderItems", portalOrderMapper.selectPortalOrderItemsByOrderNo(order.getOrderNo()));
        return data;
    }

    @Override
    public int completeOrder(Long orderId)
    {
        checkPending(orderId);
        return portalOrderMapper.updateOrderStatus(orderId, "completed");
    }

    @Override
    @Transactional
    public int cancelOrder(Long orderId)
    {
        PortalOrder order = checkPending(orderId);
        // 回补库存（先回补后改状态，同一事务内失败即整体回滚）
        List<PortalOrderItem> items = portalOrderMapper.selectPortalOrderItemsByOrderNo(order.getOrderNo());
        for (PortalOrderItem item : items)
        {
            portalOrderMapper.restoreStock(item.getProductId(), item.getQuantity());
        }
        return portalOrderMapper.updateOrderStatus(orderId, "cancelled");
    }

    /**
     * 校验订单存在且为 pending，否则抛业务异常
     */
    private PortalOrder checkPending(Long orderId)
    {
        PortalOrder order = portalOrderMapper.selectOrderById(orderId);
        if (order == null)
        {
            throw new ServiceException("订单不存在");
        }
        if (!"pending".equals(order.getStatus()))
        {
            throw new ServiceException("订单状态不允许该操作");
        }
        return order;
    }
}
