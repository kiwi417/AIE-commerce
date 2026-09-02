package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallCategory;

/**
 * 商城商品分类Mapper接口
 *
 * @author ruoyi
 */
public interface MallCategoryMapper
{
    /**
     * 查询分类列表
     *
     * @param category 过滤条件
     * @return 分类集合
     */
    public List<MallCategory> selectMallCategoryList(MallCategory category);

    /**
     * 按主键查询分类
     *
     * @param categoryId 分类id
     * @return 分类
     */
    public MallCategory selectMallCategoryById(Long categoryId);

    /**
     * 新增分类
     *
     * @param category 分类
     * @return 结果
     */
    public int insertMallCategory(MallCategory category);

    /**
     * 修改分类
     *
     * @param category 分类
     * @return 结果
     */
    public int updateMallCategory(MallCategory category);

    /**
     * 批量删除分类
     *
     * @param categoryIds 分类id数组
     * @return 结果
     */
    public int deleteMallCategoryByIds(Long[] categoryIds);
}
