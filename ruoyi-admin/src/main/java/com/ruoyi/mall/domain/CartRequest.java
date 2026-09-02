package com.ruoyi.mall.domain;

import jakarta.validation.constraints.NotBlank;

/**
 * 购物车请求体（add/update/remove/clear 共用；clear 只传 sessionId，
 * 故 productId/quantity 不在此处校验，由 service 按接口语义检查）
 *
 * @author ruoyi
 */
public class CartRequest
{
    /** 会话id（前端本地生成的不透明字符串） */
    @NotBlank(message = "会话不能为空")
    private String sessionId;

    /** 商品id（add/update/remove 必传） */
    private Long productId;

    /** 数量（add/update 必传） */
    private Integer quantity;

    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }
}
