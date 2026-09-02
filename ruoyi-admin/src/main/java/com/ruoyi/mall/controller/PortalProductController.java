package com.ruoyi.mall.controller;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.service.IMallProductService;

/**
 * 门户商品接口（免登录）
 *
 * @author ruoyi
 */
@Anonymous
@RestController
@RequestMapping("/portal/product")
public class PortalProductController
{
    private final IMallProductService mallProductService;

    public PortalProductController(IMallProductService mallProductService)
    {
        this.mallProductService = mallProductService;
    }

    /**
     * 商品分页（前端契约：顶层 rows/total，不包 data；categoryId 前端"全部"传空串，
     * 用 String 接收再转 Long，避免 Spring 类型转换异常）
     */
    @GetMapping("/page")
    public TableDataInfo page(@RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String categoryId)
    {
        return mallProductService.selectMallProductPage(pageNum, pageSize, productName, parseCategoryId(categoryId));
    }

    /**
     * 商品搜索（与分页同语义，api 契约补全）
     */
    @GetMapping("/search")
    public TableDataInfo search(@RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String categoryId)
    {
        return mallProductService.selectMallProductPage(pageNum, pageSize, productName, parseCategoryId(categoryId));
    }

    /**
     * 商品详情
     */
    @GetMapping("/{productId}")
    public AjaxResult detail(@PathVariable Long productId)
    {
        MallProduct product = mallProductService.selectMallProductById(productId);
        return AjaxResult.success(product);
    }

    private Long parseCategoryId(String categoryId)
    {
        if (!StringUtils.hasText(categoryId))
        {
            return null;
        }
        try
        {
            return Long.valueOf(categoryId);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }
}
