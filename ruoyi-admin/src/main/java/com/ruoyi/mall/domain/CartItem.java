package com.ruoyi.mall.domain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车条目对象（会话购物车，Redis 存储的商品快照）
 *
 * @author ruoyi
 */
public class CartItem implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 商品id */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 单价 */
    private BigDecimal price;

    /** 数量 */
    private Integer quantity;

    /** 货架位置 */
    private String shelfArea;

    /** 单位 */
    private String unit;

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public BigDecimal getPrice()
    {
        return price;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }

    public String getShelfArea()
    {
        return shelfArea;
    }

    public void setShelfArea(String shelfArea)
    {
        this.shelfArea = shelfArea;
    }

    public String getUnit()
    {
        return unit;
    }

    public void setUnit(String unit)
    {
        this.unit = unit;
    }
}
