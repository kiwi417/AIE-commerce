package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.MallCategory;

/**
 * 商城商品分类Service接口
 *
 * @author ruoyi
 */
public interface IMallCategoryService
{
    /**
     * 查询上架分类列表（带 Redis 缓存，故障降级直查库）
     *
     * @return 分类集合
     */
    public List<MallCategory> selectMallCategoryList();

    /**
     * 管理端：查询全部分类（含停用，不走缓存）
     */
    public List<MallCategory> selectMallCategoryListAll();

    /**
     * 按主键查询分类
     */
    public MallCategory selectMallCategoryById(Long categoryId);

    /**
     * 新增分类（写操作自动清缓存）
     */
    public int insertMallCategory(MallCategory category);

    /**
     * 修改分类（写操作自动清缓存）
     */
    public int updateMallCategory(MallCategory category);

    /**
     * 批量删除分类（分类下有商品时拒绝删除）
     */
    public int deleteMallCategoryByIds(Long[] categoryIds);
}
