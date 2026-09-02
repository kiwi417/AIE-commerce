package com.ruoyi.mall.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.constant.HttpStatus;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.mall.constant.MallCacheConstants;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.mapper.MallProductMapper;
import com.ruoyi.mall.service.IMallProductService;

/**
 * 商城商品Service业务层处理：Redis 缓存优先，故障降级直查数据库
 *
 * @author ruoyi
 */
@Service
public class MallProductServiceImpl implements IMallProductService
{
    private static final Logger log = LoggerFactory.getLogger(MallProductServiceImpl.class);

    private final MallProductMapper mallProductMapper;

    private final RedisCache redisCache;

    public MallProductServiceImpl(MallProductMapper mallProductMapper, RedisCache redisCache)
    {
        this.mallProductMapper = mallProductMapper;
        this.redisCache = redisCache;
    }

    @Override
    public TableDataInfo selectMallProductPage(int pageNum, int pageSize, String productName, Long categoryId)
    {
        String name = productName == null ? "" : productName.trim();
        String key = MallCacheConstants.MALL_PRODUCT_PAGE_PREFIX + pageNum + ":" + pageSize + ":" + name + ":"
                + (categoryId == null ? "" : categoryId);

        try
        {
            Object cached = redisCache.getCacheObject(key);
            if (cached instanceof String)
            {
                return JSON.parseObject((String) cached, TableDataInfo.class);
            }
        }
        catch (Exception e)
        {
            log.warn("读取商品分页缓存失败，降级查询数据库: {}", e.getMessage());
        }

        PageHelper.startPage(pageNum, pageSize);
        MallProduct query = new MallProduct();
        query.setStatus("0");
        if (categoryId != null)
        {
            query.setCategoryId(categoryId);
        }
        if (!name.isEmpty())
        {
            query.setProductName(name);
        }
        List<MallProduct> list = mallProductMapper.selectMallProductList(query);
        long total = new PageInfo<>(list).getTotal();

        TableDataInfo result = new TableDataInfo(list, (int) total);
        result.setCode(HttpStatus.SUCCESS);
        result.setMsg("查询成功");

        if (total > 0)
        {
            try
            {
                redisCache.setCacheObject(key, JSON.toJSONString(result),
                        MallCacheConstants.PRODUCT_PAGE_TTL_MINUTES, TimeUnit.MINUTES);
            }
            catch (Exception e)
            {
                log.warn("写入商品分页缓存失败: {}", e.getMessage());
            }
        }
        return result;
    }

    @Override
    public MallProduct selectMallProductById(Long productId)
    {
        if (productId == null)
        {
            throw new ServiceException("商品不存在");
        }
        String key = MallCacheConstants.MALL_PRODUCT_DETAIL_PREFIX + productId;

        try
        {
            Object cached = redisCache.getCacheObject(key);
            if (cached instanceof String)
            {
                return JSON.parseObject((String) cached, MallProduct.class);
            }
        }
        catch (Exception e)
        {
            log.warn("读取商品详情缓存失败，降级查询数据库: {}", e.getMessage());
        }

        MallProduct product = mallProductMapper.selectMallProductById(productId);
        if (product == null || !"0".equals(product.getStatus()))
        {
            throw new ServiceException("商品不存在");
        }

        try
        {
            redisCache.setCacheObject(key, JSON.toJSONString(product),
                    MallCacheConstants.PRODUCT_DETAIL_TTL_MINUTES, TimeUnit.MINUTES);
        }
        catch (Exception e)
        {
            log.warn("写入商品详情缓存失败: {}", e.getMessage());
        }
        return product;
    }

    // ==================== 管理端 ====================

    @Override
    public List<MallProduct> selectMallProductListAdmin(MallProduct product)
    {
        return mallProductMapper.selectMallProductList(product);
    }

    @Override
    public MallProduct selectMallProductAdminById(Long productId)
    {
        MallProduct product = mallProductMapper.selectMallProductById(productId);
        if (product == null)
        {
            throw new ServiceException("商品不存在");
        }
        return product;
    }

    @Override
    public int insertMallProduct(MallProduct product)
    {
        int rows = mallProductMapper.insertMallProduct(product);
        evictProductCaches(product.getProductId());
        return rows;
    }

    @Override
    public int updateMallProduct(MallProduct product)
    {
        int rows = mallProductMapper.updateMallProduct(product);
        evictProductCaches(product.getProductId());
        return rows;
    }

    @Override
    public int deleteMallProductByIds(Long[] productIds)
    {
        int rows = mallProductMapper.deleteMallProductByIds(productIds);
        for (Long id : productIds)
        {
            evictProductCaches(id);
        }
        return rows;
    }

    /**
     * 写操作后清理商品相关缓存：详情 + 全部分页（写失败仅告警，不阻塞业务）。
     * 注意：Python 向量知识库最多 10 分钟内自动重同步，无需在此触发。
     */
    private void evictProductCaches(Long productId)
    {
        try
        {
            if (productId != null)
            {
                redisCache.deleteObject(MallCacheConstants.MALL_PRODUCT_DETAIL_PREFIX + productId);
            }
            redisCache.keys(MallCacheConstants.MALL_PRODUCT_PAGE_PREFIX + "*")
                    .forEach(redisCache::deleteObject);
        }
        catch (Exception e)
        {
            log.warn("清理商品缓存失败: {}", e.getMessage());
        }
    }
}
