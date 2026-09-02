package com.ruoyi.mall.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.mall.constant.MallCacheConstants;
import com.ruoyi.mall.domain.CartItem;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.service.IMallProductService;
import com.ruoyi.mall.service.IPortalCartService;

/**
 * 门户购物车Service业务层处理：整单 JSON 数组存 Redis（TTL 每次写刷新=滑动过期），
 * 商品快照经详情缓存取数；购物车无 MySQL 兜底，Redis 故障抛友好文案。
 *
 * @author ruoyi
 */
@Service
public class PortalCartServiceImpl implements IPortalCartService
{
    private static final Logger log = LoggerFactory.getLogger(PortalCartServiceImpl.class);

    private final RedisCache redisCache;

    private final IMallProductService mallProductService;

    public PortalCartServiceImpl(RedisCache redisCache, IMallProductService mallProductService)
    {
        this.redisCache = redisCache;
        this.mallProductService = mallProductService;
    }

    @Override
    public void addItem(String sessionId, Long productId, Integer quantity)
    {
        if (productId == null)
        {
            throw new ServiceException("参数错误");
        }
        int qty = quantity == null ? 1 : quantity;
        if (qty <= 0)
        {
            throw new ServiceException("数量必须大于0");
        }

        MallProduct product = mallProductService.selectMallProductById(productId);
        CartItem snapshot = snapshot(product, qty);

        List<CartItem> cart = getCart(sessionId);
        boolean exists = false;
        for (CartItem item : cart)
        {
            if (item.getProductId().equals(productId))
            {
                item.setQuantity(item.getQuantity() + qty);
                exists = true;
                break;
            }
        }
        if (!exists)
        {
            cart.add(snapshot);
        }
        saveCart(sessionId, cart);
    }

    @Override
    public List<CartItem> listItems(String sessionId)
    {
        return getCart(sessionId);
    }

    @Override
    public void updateItem(String sessionId, Long productId, Integer quantity)
    {
        if (productId == null || quantity == null)
        {
            throw new ServiceException("参数错误");
        }
        if (quantity <= 0)
        {
            removeItem(sessionId, productId);
            return;
        }
        List<CartItem> cart = getCart(sessionId);
        for (CartItem item : cart)
        {
            if (item.getProductId().equals(productId))
            {
                item.setQuantity(quantity);
                saveCart(sessionId, cart);
                return;
            }
        }
        throw new ServiceException("购物车中不存在该商品");
    }

    @Override
    public void removeItem(String sessionId, Long productId)
    {
        if (productId == null)
        {
            throw new ServiceException("参数错误");
        }
        List<CartItem> cart = getCart(sessionId);
        cart.removeIf(item -> item.getProductId().equals(productId));
        saveCart(sessionId, cart);
    }

    @Override
    public void clear(String sessionId)
    {
        try
        {
            redisCache.deleteObject(cartKey(sessionId));
        }
        catch (Exception e)
        {
            log.warn("清空购物车失败: {}", e.getMessage());
            throw new ServiceException("购物车服务暂不可用，请稍后重试");
        }
    }

    /**
     * 读取整单（未知会话返回空列表；Redis 故障抛业务异常，购物车无 DB 兜底属设计）
     */
    private List<CartItem> getCart(String sessionId)
    {
        try
        {
            Object cached = redisCache.getCacheObject(cartKey(sessionId));
            if (cached instanceof String)
            {
                List<CartItem> items = JSON.parseArray((String) cached, CartItem.class);
                return items == null ? new ArrayList<>() : items;
            }
            return new ArrayList<>();
        }
        catch (Exception e)
        {
            log.warn("读取购物车失败: {}", e.getMessage());
            throw new ServiceException("购物车服务暂不可用，请稍后重试");
        }
    }

    private void saveCart(String sessionId, List<CartItem> cart)
    {
        try
        {
            redisCache.setCacheObject(cartKey(sessionId), JSON.toJSONString(cart),
                    MallCacheConstants.CART_TTL_DAYS, TimeUnit.DAYS);
        }
        catch (Exception e)
        {
            log.warn("写入购物车失败: {}", e.getMessage());
            throw new ServiceException("购物车服务暂不可用，请稍后重试");
        }
    }

    private CartItem snapshot(MallProduct product, int quantity)
    {
        CartItem item = new CartItem();
        item.setProductId(product.getProductId());
        item.setProductName(product.getProductName());
        item.setPrice(product.getPrice());
        item.setQuantity(quantity);
        item.setShelfArea(product.getShelfArea());
        item.setUnit(product.getUnit());
        return item;
    }

    private String cartKey(String sessionId)
    {
        return MallCacheConstants.MALL_CART_PREFIX + sessionId;
    }
}
