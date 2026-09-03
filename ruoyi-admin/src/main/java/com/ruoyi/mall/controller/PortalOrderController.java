package com.ruoyi.mall.controller;

import java.util.List;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.mall.domain.OrderCreateRequest;
import com.ruoyi.mall.domain.PortalOrder;
import com.ruoyi.mall.service.IPortalOrderService;

/**
 * 门户订单接口（免登录：下单/查询/详情）
 *
 * @author ruoyi
 */
@Anonymous
@RestController
@RequestMapping("/portal/order")
public class PortalOrderController
{
    private final IPortalOrderService portalOrderService;

    public PortalOrderController(IPortalOrderService portalOrderService)
    {
        this.portalOrderService = portalOrderService;
    }

    /**
     * 提交订单（订单内容以购物车快照为准）
     */
    @PostMapping("/create")
    public AjaxResult create(@Validated @RequestBody OrderCreateRequest request)
    {
        Map<String, Object> order = portalOrderService.createOrder(request);
        return AjaxResult.success(order);
    }

    /**
     * 订单查询（手机号/订单号过滤，两者皆空返回空列表）
     */
    @GetMapping("/query")
    public AjaxResult query(@RequestParam(required = false) String phone,
                            @RequestParam(required = false) String orderNo)
    {
        List<PortalOrder> orders = portalOrderService.queryOrders(phone, orderNo);
        return AjaxResult.success(orders);
    }

    /**
     * 订单详情（订单 + 明细）
     */
    @GetMapping("/{orderNo}")
    public AjaxResult detail(@PathVariable("orderNo") String orderNo)
    {
        Map<String, Object> data = portalOrderService.getOrderByOrderNo(orderNo);
        return AjaxResult.success(data);
    }
}
