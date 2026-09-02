package com.ruoyi.mall.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.mall.domain.MallCategory;
import com.ruoyi.mall.service.IMallCategoryService;

/**
 * 商城商品分类管理（后台管理端，需登录 + 权限）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/mall/category")
public class MallCategoryController extends BaseController
{
    @Autowired
    private IMallCategoryService mallCategoryService;

    /**
     * 查询分类列表（含停用分类，供管理端使用）
     */
    @PreAuthorize("@ss.hasPermi('mall:category:list')")
    @GetMapping("/list")
    public TableDataInfo list()
    {
        List<MallCategory> list = mallCategoryService.selectMallCategoryListAll();
        return getDataTable(list);
    }

    /**
     * 获取分类详细信息
     */
    @PreAuthorize("@ss.hasPermi('mall:category:query')")
    @GetMapping(value = "/{categoryId}")
    public AjaxResult getInfo(@PathVariable("categoryId") Long categoryId)
    {
        return success(mallCategoryService.selectMallCategoryById(categoryId));
    }

    /**
     * 新增分类
     */
    @PreAuthorize("@ss.hasPermi('mall:category:add')")
    @Log(title = "商品分类管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallCategory category)
    {
        return toAjax(mallCategoryService.insertMallCategory(category));
    }

    /**
     * 修改分类
     */
    @PreAuthorize("@ss.hasPermi('mall:category:edit')")
    @Log(title = "商品分类管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallCategory category)
    {
        return toAjax(mallCategoryService.updateMallCategory(category));
    }

    /**
     * 删除分类（分类下有商品时拒绝删除）
     */
    @PreAuthorize("@ss.hasPermi('mall:category:remove')")
    @Log(title = "商品分类管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryIds}")
    public AjaxResult remove(@PathVariable Long[] categoryIds)
    {
        return toAjax(mallCategoryService.deleteMallCategoryByIds(categoryIds));
    }
}
