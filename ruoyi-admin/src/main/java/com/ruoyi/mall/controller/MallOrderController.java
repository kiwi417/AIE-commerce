package com.ruoyi.mall.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.mall.domain.OrderQueryParam;
import com.ruoyi.mall.domain.PortalOrder;
import com.ruoyi.mall.service.IMallOrderService;

/**
 * 订单管理（后台管理端，需登录 + 权限）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/business/order")
public class MallOrderController extends BaseController
{
    @Autowired
    private IMallOrderService mallOrderService;

    /**
     * 订单列表（分页；orderNo 模糊/customerPhone/status 过滤）
     */
    @PreAuthorize("@ss.hasPermi('business:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderQueryParam query)
    {
        startPage();
        List<PortalOrder> list = mallOrderService.selectOrderList(query);
        return getDataTable(list);
    }

    /**
     * 订单详情（订单 + orderItems 明细）
     */
    @PreAuthorize("@ss.hasPermi('business:order:query')")
    @GetMapping("/{orderId}")
    public AjaxResult getInfo(@PathVariable("orderId") Long orderId)
    {
        Map<String, Object> data = mallOrderService.selectOrderById(orderId);
        return success(data);
    }

    /**
     * 完成订单（仅待取货可完成）
     */
    @PreAuthorize("@ss.hasPermi('business:order:complete')")
    @Log(title = "订单管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{orderId}/complete")
    public AjaxResult complete(@PathVariable("orderId") Long orderId)
    {
        return toAjax(mallOrderService.completeOrder(orderId));
    }

    /**
     * 取消订单（仅待取货可取消，回补库存）
     */
    @PreAuthorize("@ss.hasPermi('business:order:cancel')")
    @Log(title = "订单管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{orderId}/cancel")
    public AjaxResult cancel(@PathVariable("orderId") Long orderId)
    {
        return toAjax(mallOrderService.cancelOrder(orderId));
    }
}
