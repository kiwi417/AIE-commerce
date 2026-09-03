package com.ruoyi.mall.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 商城订单明细对象 mall_order_item（商品快照，不随商品表变更）
 *
 * @author ruoyi
 */
public class PortalOrderItem
{
    private static final long serialVersionUID = 1L;

    /** 明细id */
    private Long itemId;

    /** 订单号 */
    private String orderNo;

    /** 商品id */
    private Long productId;

    /** 商品名称（快照） */
    private String productName;

    /** 成交单价（快照） */
    private BigDecimal price;

    /** 购买数量 */
    private Integer quantity;

    /** 小计 */
    private BigDecimal subtotal;

    /** 货架位置（快照） */
    private String shelfArea;

    /** 单位（快照） */
    private String unit;

    public Long getItemId()
    {
        return itemId;
    }

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

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

    public BigDecimal getSubtotal()
    {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal)
    {
        this.subtotal = subtotal;
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

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("itemId", getItemId())
            .append("orderNo", getOrderNo())
            .append("productId", getProductId())
            .append("productName", getProductName())
            .append("quantity", getQuantity())
            .toString();
    }
}
