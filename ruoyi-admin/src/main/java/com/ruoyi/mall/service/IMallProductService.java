package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.mall.domain.MallProduct;

/**
 * 商城商品Service接口
 *
 * @author ruoyi
 */
public interface IMallProductService
{
    /**
     * 分页查询上架商品（带 Redis 缓存，故障降级直查库）
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param productName 商品名称（模糊）
     * @param categoryId 分类id（可空）
     * @return 分页结果（顶层 rows/total，直接序列化为前端契约）
     */
    public TableDataInfo selectMallProductPage(int pageNum, int pageSize, String productName, Long categoryId);

    /**
     * 按主键查询上架商品（带 Redis 缓存）
     *
     * @param productId 商品id
     * @return 商品
     */
    public MallProduct selectMallProductById(Long productId);

    /**
     * 管理端：查询商品列表（含下架，不走缓存，配合 PageHelper）
     */
    public List<MallProduct> selectMallProductListAdmin(MallProduct product);

    /**
     * 管理端：按主键查询商品（含下架，不走缓存）
     */
    public MallProduct selectMallProductAdminById(Long productId);

    /**
     * 新增商品（写操作自动清缓存）
     */
    public int insertMallProduct(MallProduct product);

    /**
     * 修改商品（写操作自动清缓存）
     */
    public int updateMallProduct(MallProduct product);

    /**
     * 批量删除商品（写操作自动清缓存）
     */
    public int deleteMallProductByIds(Long[] productIds);
}
