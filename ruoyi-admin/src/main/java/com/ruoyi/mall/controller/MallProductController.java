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
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.service.IMallProductService;

/**
 * 商城商品管理（后台管理端，需登录 + 权限）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/mall/product")
public class MallProductController extends BaseController
{
    @Autowired
    private IMallProductService mallProductService;

    /**
     * 查询商品列表（含下架商品、分类名，供管理端使用）
     */
    @PreAuthorize("@ss.hasPermi('mall:product:list')")
    @GetMapping("/list")
    public TableDataInfo list(MallProduct product)
    {
        startPage();
        List<MallProduct> list = mallProductService.selectMallProductListAdmin(product);
        return getDataTable(list);
    }

    /**
     * 获取商品详细信息
     */
    @PreAuthorize("@ss.hasPermi('mall:product:query')")
    @GetMapping(value = "/{productId}")
    public AjaxResult getInfo(@PathVariable("productId") Long productId)
    {
        return success(mallProductService.selectMallProductAdminById(productId));
    }

    /**
     * 新增商品
     */
    @PreAuthorize("@ss.hasPermi('mall:product:add')")
    @Log(title = "商品管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallProduct product)
    {
        return toAjax(mallProductService.insertMallProduct(product));
    }

    /**
     * 修改商品
     */
    @PreAuthorize("@ss.hasPermi('mall:product:edit')")
    @Log(title = "商品管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallProduct product)
    {
        return toAjax(mallProductService.updateMallProduct(product));
    }

    /**
     * 删除商品
     */
    @PreAuthorize("@ss.hasPermi('mall:product:remove')")
    @Log(title = "商品管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{productIds}")
    public AjaxResult remove(@PathVariable Long[] productIds)
    {
        return toAjax(mallProductService.deleteMallProductByIds(productIds));
    }
}
