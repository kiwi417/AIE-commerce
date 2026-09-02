package com.ruoyi.mall.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商城商品对象 mall_product
 *
 * @author ruoyi
 */
public class MallProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 商品id */
    private Long productId;

    /** 分类id */
    private Long categoryId;

    /** 分类名称（列表联查展示用，非表字段） */
    private String categoryName;

    /** 商品名称 */
    private String productName;

    /** 价格 */
    private BigDecimal price;

    /** 库存 */
    private Integer stock;

    /** 单位（件/瓶/盒/包等） */
    private String unit;

    /** 条码 */
    private String barcode;

    /** 货架位置 */
    private String shelfArea;

    /** 商品描述 */
    private String description;

    /** 标签（逗号分隔，检索与展示） */
    private String tags;

    /** 图片地址 */
    private String imageUrl;

    /** 状态（0上架 1下架） */
    private String status;

    /** 前端契约别名：列表与AI商品卡片都读 p.id */
    public Long getId()
    {
        return productId;
    }

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
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

    public Integer getStock()
    {
        return stock;
    }

    public void setStock(Integer stock)
    {
        this.stock = stock;
    }

    public String getUnit()
    {
        return unit;
    }

    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    public String getBarcode()
    {
        return barcode;
    }

    public void setBarcode(String barcode)
    {
        this.barcode = barcode;
    }

    public String getShelfArea()
    {
        return shelfArea;
    }

    public void setShelfArea(String shelfArea)
    {
        this.shelfArea = shelfArea;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getTags()
    {
        return tags;
    }

    public void setTags(String tags)
    {
        this.tags = tags;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("productId", getProductId())
            .append("categoryId", getCategoryId())
            .append("productName", getProductName())
            .append("price", getPrice())
            .append("stock", getStock())
            .append("status", getStatus())
            .toString();
    }
}
