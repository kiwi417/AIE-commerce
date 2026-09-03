package com.ruoyi.mall.service;

import java.util.List;
import java.util.Map;

import com.ruoyi.mall.domain.OrderCreateRequest;
import com.ruoyi.mall.domain.PortalOrder;

/**
 * 门户订单Service接口（免登录下单/查询）
 *
 * @author ruoyi
 */
public interface IPortalOrderService
{
    /**
     * 提交订单：以购物车快照为订单内容，原子扣减库存，落库后清空购物车
     *
     * @param request 下单请求
     * @return 订单信息（orderNo/phoneMasked/totalAmount 等，供前端下单成功页展示）
     */
    public Map<String, Object> createOrder(OrderCreateRequest request);

    /**
     * 门户订单查询（手机号/订单号过滤；两者皆空返回空列表，不返回全量）
     *
     * @param phone 取货手机号（可空）
     * @param orderNo 订单号（可空）
     * @return 订单集合
     */
    public List<PortalOrder> queryOrders(String phone, String orderNo);

    /**
     * 按订单号查询订单详情（订单 + 明细）
     *
     * @param orderNo 订单号
     * @return {"order": 订单, "items": 明细集合}
     */
    public Map<String, Object> getOrderByOrderNo(String orderNo);
}
