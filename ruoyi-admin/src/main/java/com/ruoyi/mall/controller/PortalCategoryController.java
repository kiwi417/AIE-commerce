package com.ruoyi.mall.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.mall.domain.MallCategory;
import com.ruoyi.mall.service.IMallCategoryService;

/**
 * 门户商品分类接口（免登录）
 *
 * @author ruoyi
 */
@Anonymous
@RestController
@RequestMapping("/portal/category")
public class PortalCategoryController
{
    private final IMallCategoryService mallCategoryService;

    public PortalCategoryController(IMallCategoryService mallCategoryService)
    {
        this.mallCategoryService = mallCategoryService;
    }

    /**
     * 分类列表
     */
    @GetMapping("/list")
    public AjaxResult list()
    {
        List<MallCategory> list = mallCategoryService.selectMallCategoryList();
        return AjaxResult.success(list);
    }
}
