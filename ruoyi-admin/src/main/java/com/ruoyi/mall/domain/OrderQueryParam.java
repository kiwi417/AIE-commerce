package com.ruoyi.mall.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 管理端订单列表查询条件（继承 BaseEntity 以复用 params 分页参数）
 *
 * @author ruoyi
 */
public class OrderQueryParam extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 订单号（模糊） */
    private String orderNo;

    /** 取货手机号 */
    private String customerPhone;

    /** 状态（pending/confirmed/completed/cancelled） */
    private String status;

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public String getCustomerPhone()
    {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone)
    {
        this.customerPhone = customerPhone;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
