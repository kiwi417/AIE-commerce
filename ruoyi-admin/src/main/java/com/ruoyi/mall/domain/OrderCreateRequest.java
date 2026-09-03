package com.ruoyi.mall.domain;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * 门户下单请求体（订单内容以购物车快照为准，items 仅作非空校验，防价格/数量篡改）
 *
 * @author ruoyi
 */
public class OrderCreateRequest
{
    /** 会话id（购物车所在会话） */
    @NotBlank(message = "会话不能为空")
    private String sessionId;

    /** 取货手机号 */
    @NotBlank(message = "取货手机号不能为空")
    private String phone;

    /** 取货人姓名（选填） */
    private String customerName;

    /** 下单条目（productId + quantity） */
    @NotEmpty(message = "订单商品不能为空")
    @Valid
    private List<OrderItemRequest> items;

    /** 备注（选填） */
    private String remark;

    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getCustomerName()
    {
        return customerName;
    }

    public void setCustomerName(String customerName)
    {
        this.customerName = customerName;
    }

    public List<OrderItemRequest> getItems()
    {
        return items;
    }

    public void setItems(List<OrderItemRequest> items)
    {
        this.items = items;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    /**
     * 下单条目
     */
    public static class OrderItemRequest
    {
        /** 商品id */
        private Long productId;

        /** 数量 */
        private Integer quantity;

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
}
