package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.mall.domain.CartItem;
import com.ruoyi.mall.domain.OrderCreateRequest;
import com.ruoyi.mall.domain.PortalOrder;
import com.ruoyi.mall.domain.PortalOrderItem;
import com.ruoyi.mall.mapper.PortalOrderMapper;
import com.ruoyi.mall.service.IPortalCartService;
import com.ruoyi.mall.service.IPortalOrderService;

/**
 * 门户订单Service业务层处理：
 * 订单内容以购物车 Redis 快照为准（防客户端篡改价格/数量），
 * 下单原子扣减库存并落库，失败整体回滚；成功后清空购物车（清车失败不阻断下单）。
 *
 * @author ruoyi
 */
@Service
public class PortalOrderServiceImpl implements IPortalOrderService
{
    private static final Logger log = LoggerFactory.getLogger(PortalOrderServiceImpl.class);

    /** 取货手机号校验（前端同款规则） */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    private final PortalOrderMapper portalOrderMapper;

    private final IPortalCartService portalCartService;

    public PortalOrderServiceImpl(PortalOrderMapper portalOrderMapper, IPortalCartService portalCartService)
    {
        this.portalOrderMapper = portalOrderMapper;
        this.portalCartService = portalCartService;
    }

    @Override
    @Transactional
    public Map<String, Object> createOrder(OrderCreateRequest request)
    {
        String phone = request.getPhone();
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches())
        {
            throw new ServiceException("取货手机号格式不正确");
        }

        List<CartItem> cart = portalCartService.listItems(request.getSessionId());
        if (cart == null || cart.isEmpty())
        {
            throw new ServiceException("购物车为空，请先添加商品");
        }

        // 1. 原子扣库存：任一商品库存不足即整体回滚
        for (CartItem item : cart)
        {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0)
            {
                throw new ServiceException("购物车数据异常，请返回购物车确认");
            }
            int rows = portalOrderMapper.deductStock(item.getProductId(), item.getQuantity());
            if (rows == 0)
            {
                throw new ServiceException("「" + item.getProductName() + "」库存不足，请调整数量");
            }
        }

        // 2. 组装订单（金额/名称取购物车快照）
        String orderNo = generateOrderNo();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int itemCount = 0;
        List<PortalOrderItem> orderItems = new ArrayList<>();
        for (CartItem item : cart)
        {
            BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
            itemCount += item.getQuantity();

            PortalOrderItem orderItem = new PortalOrderItem();
            orderItem.setOrderNo(orderNo);
            orderItem.setProductId(item.getProductId());
            orderItem.setProductName(item.getProductName());
            orderItem.setPrice(price);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setSubtotal(subtotal);
            orderItem.setShelfArea(item.getShelfArea());
            orderItem.setUnit(item.getUnit());
            orderItems.add(orderItem);
        }

        PortalOrder order = new PortalOrder();
        order.setOrderNo(orderNo);
        order.setSessionId(request.getSessionId());
        order.setCustomerPhone(phone);
        order.setCustomerName(request.getCustomerName());
        order.setTotalAmount(totalAmount);
        order.setItemCount(itemCount);
        order.setStatus("pending");
        order.setRemark(request.getRemark());
        portalOrderMapper.insertPortalOrder(order);
        portalOrderMapper.insertPortalOrderItems(orderItems);

        // 3. 清空购物车（Redis 操作不参与事务，失败仅告警）
        try
        {
            portalCartService.clear(request.getSessionId());
        }
        catch (Exception e)
        {
            log.warn("下单成功但清空购物车失败: {}", e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("phoneMasked", maskPhone(phone));
        result.put("totalAmount", totalAmount);
        result.put("itemCount", itemCount);
        result.put("status", order.getStatus());
        result.put("customerPhone", phone);
        result.put("customerName", request.getCustomerName());
        result.put("createTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        return result;
    }

    @Override
    public List<PortalOrder> queryOrders(String phone, String orderNo)
    {
        if ((phone == null || phone.isEmpty()) && (orderNo == null || orderNo.isEmpty()))
        {
            return new ArrayList<>();
        }
        PortalOrder query = new PortalOrder();
        query.setCustomerPhone(phone);
        query.setOrderNo(orderNo);
        return portalOrderMapper.selectPortalOrderList(query);
    }

    @Override
    public Map<String, Object> getOrderByOrderNo(String orderNo)
    {
        PortalOrder order = portalOrderMapper.selectPortalOrderByOrderNo(orderNo);
        if (order == null)
        {
            throw new ServiceException("订单不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", portalOrderMapper.selectPortalOrderItemsByOrderNo(orderNo));
        return data;
    }

    /**
     * 订单号：yyyyMMddHHmmss + 4位随机数（唯一索引兜底）
     */
    private String generateOrderNo()
    {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return timestamp + random;
    }

    /**
     * 手机号掩码：138****8000
     */
    private String maskPhone(String phone)
    {
        if (phone.length() != 11)
        {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
