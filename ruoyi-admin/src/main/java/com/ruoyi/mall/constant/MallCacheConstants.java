package com.ruoyi.mall.constant;

/**
 * 商城模块 Redis 缓存键常量（命名空间 mall:*，与 Python 端 ai:* 不重叠）
 *
 * @author ruoyi
 */
public class MallCacheConstants
{
    /** 分类列表缓存键 */
    public static final String MALL_CATEGORY_LIST_KEY = "mall:category:list";

    /** 商品分页缓存键前缀 */
    public static final String MALL_PRODUCT_PAGE_PREFIX = "mall:product:page:";

    /** 商品详情缓存键前缀 */
    public static final String MALL_PRODUCT_DETAIL_PREFIX = "mall:product:detail:";

    /** 购物车缓存键前缀 */
    public static final String MALL_CART_PREFIX = "mall:cart:";

    /** 分类列表缓存时长（分钟） */
    public static final int CATEGORY_LIST_TTL_MINUTES = 10;

    /** 商品分页缓存时长（分钟） */
    public static final int PRODUCT_PAGE_TTL_MINUTES = 5;

    /** 商品详情缓存时长（分钟） */
    public static final int PRODUCT_DETAIL_TTL_MINUTES = 10;

    /** 购物车缓存时长（天） */
    public static final int CART_TTL_DAYS = 7;
}
