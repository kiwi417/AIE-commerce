package com.ruoyi.mall.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.mall.domain.CartItem;
import com.ruoyi.mall.domain.CartRequest;
import com.ruoyi.mall.service.IPortalCartService;

/**
 * 门户购物车接口（免登录，会话购物车）
 *
 * @author ruoyi
 */
@Anonymous
@RestController
@RequestMapping("/portal/cart")
public class PortalCartController
{
    private final IPortalCartService portalCartService;

    public PortalCartController(IPortalCartService portalCartService)
    {
        this.portalCartService = portalCartService;
    }

    /**
     * 加入购物车
     */
    @PostMapping("/add")
    public AjaxResult add(@Validated @RequestBody CartRequest request)
    {
        portalCartService.addItem(request.getSessionId(), request.getProductId(), request.getQuantity());
        return AjaxResult.success();
    }

    /**
     * 购物车列表
     */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam String sessionId)
    {
        List<CartItem> items = portalCartService.listItems(sessionId);
        Map<String, Object> data = new HashMap<>();
        data.put("cartItems", items);
        return AjaxResult.success(data);
    }

    /**
     * 更新数量
     */
    @PutMapping("/update")
    public AjaxResult update(@Validated @RequestBody CartRequest request)
    {
        portalCartService.updateItem(request.getSessionId(), request.getProductId(), request.getQuantity());
        return AjaxResult.success();
    }

    /**
     * 移除条目（axios delete 带 body）
     */
    @DeleteMapping("/remove")
    public AjaxResult remove(@Validated @RequestBody CartRequest request)
    {
        portalCartService.removeItem(request.getSessionId(), request.getProductId());
        return AjaxResult.success();
    }

    /**
     * 清空购物车
     */
    @DeleteMapping("/clear")
    public AjaxResult clear(@Validated @RequestBody CartRequest request)
    {
        portalCartService.clear(request.getSessionId());
        return AjaxResult.success();
    }
}
