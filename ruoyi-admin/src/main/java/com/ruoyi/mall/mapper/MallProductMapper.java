package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallProduct;

/**
 * 商城商品Mapper接口
 *
 * @author ruoyi
 */
public interface MallProductMapper
{
    /**
     * 查询商品列表（配合 PageHelper 分页）
     *
     * @param product 过滤条件
     * @return 商品集合
     */
    public List<MallProduct> selectMallProductList(MallProduct product);

    /**
     * 按主键查询商品
     *
     * @param productId 商品id
     * @return 商品
     */
    public MallProduct selectMallProductById(Long productId);

    /**
     * 新增商品
     *
     * @param product 商品
     * @return 结果
     */
    public int insertMallProduct(MallProduct product);

    /**
     * 修改商品
     *
     * @param product 商品
     * @return 结果
     */
    public int updateMallProduct(MallProduct product);

    /**
     * 批量删除商品
     *
     * @param productIds 商品id数组
     * @return 结果
     */
    public int deleteMallProductByIds(Long[] productIds);

    /**
     * 统计分类下的商品数（删除分类前校验用）
     *
     * @param categoryId 分类id
     * @return 商品数
     */
    public int selectMallProductCountByCategoryId(Long categoryId);
}
