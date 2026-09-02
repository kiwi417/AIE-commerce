package com.ruoyi.mall.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.mall.constant.MallCacheConstants;
import com.ruoyi.mall.domain.MallCategory;
import com.ruoyi.mall.mapper.MallCategoryMapper;
import com.ruoyi.mall.mapper.MallProductMapper;
import com.ruoyi.mall.service.IMallCategoryService;

/**
 * 商城商品分类Service业务层处理：Redis 缓存优先，故障降级直查数据库
 *
 * @author ruoyi
 */
@Service
public class MallCategoryServiceImpl implements IMallCategoryService
{
    private static final Logger log = LoggerFactory.getLogger(MallCategoryServiceImpl.class);

    private final MallCategoryMapper mallCategoryMapper;

    private final MallProductMapper mallProductMapper;

    private final RedisCache redisCache;

    public MallCategoryServiceImpl(MallCategoryMapper mallCategoryMapper, MallProductMapper mallProductMapper,
            RedisCache redisCache)
    {
        this.mallCategoryMapper = mallCategoryMapper;
        this.mallProductMapper = mallProductMapper;
        this.redisCache = redisCache;
    }

    @Override
    public List<MallCategory> selectMallCategoryList()
    {
        try
        {
            Object cached = redisCache.getCacheObject(MallCacheConstants.MALL_CATEGORY_LIST_KEY);
            if (cached instanceof String)
            {
                return JSON.parseArray((String) cached, MallCategory.class);
            }
        }
        catch (Exception e)
        {
            // Redis 故障降级直查库，匿名门户流量不能 500
            log.warn("读取分类缓存失败，降级查询数据库: {}", e.getMessage());
        }

        MallCategory query = new MallCategory();
        query.setStatus("0");
        List<MallCategory> list = mallCategoryMapper.selectMallCategoryList(query);

        try
        {
            redisCache.setCacheObject(MallCacheConstants.MALL_CATEGORY_LIST_KEY, JSON.toJSONString(list),
                    MallCacheConstants.CATEGORY_LIST_TTL_MINUTES, TimeUnit.MINUTES);
        }
        catch (Exception e)
        {
            log.warn("写入分类缓存失败: {}", e.getMessage());
        }
        return list;
    }

    // ==================== 管理端 ====================

    @Override
    public List<MallCategory> selectMallCategoryListAll()
    {
        return mallCategoryMapper.selectMallCategoryList(new MallCategory());
    }

    @Override
    public MallCategory selectMallCategoryById(Long categoryId)
    {
        MallCategory category = mallCategoryMapper.selectMallCategoryById(categoryId);
        if (category == null)
        {
            throw new ServiceException("分类不存在");
        }
        return category;
    }

    @Override
    public int insertMallCategory(MallCategory category)
    {
        int rows = mallCategoryMapper.insertMallCategory(category);
        evictCategoryCache();
        return rows;
    }

    @Override
    public int updateMallCategory(MallCategory category)
    {
        int rows = mallCategoryMapper.updateMallCategory(category);
        evictCategoryCache();
        return rows;
    }

    @Override
    public int deleteMallCategoryByIds(Long[] categoryIds)
    {
        // 分类下有商品时拒绝删除，防止门户商品失去分类归属
        for (Long id : categoryIds)
        {
            if (mallProductMapper.selectMallProductCountByCategoryId(id) > 0)
            {
                throw new ServiceException("分类下存在商品，无法删除");
            }
        }
        int rows = mallCategoryMapper.deleteMallCategoryByIds(categoryIds);
        evictCategoryCache();
        return rows;
    }

    private void evictCategoryCache()
    {
        try
        {
            redisCache.deleteObject(MallCacheConstants.MALL_CATEGORY_LIST_KEY);
        }
        catch (Exception e)
        {
            log.warn("清理分类缓存失败: {}", e.getMessage());
        }
    }
}
