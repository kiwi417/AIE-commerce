package com.ruoyi.mall.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ruoyi.mall.domain.OrderQueryParam;
import com.ruoyi.mall.domain.PortalOrder;
import com.ruoyi.mall.domain.PortalOrderItem;

/**
 * 商城订单Mapper接口
 *
 * @author ruoyi
 */
public interface PortalOrderMapper
{
    /**
     * 新增订单（回填 orderId）
     *
     * @param order 订单
     * @return 结果
     */
    public int insertPortalOrder(PortalOrder order);

    /**
     * 批量新增订单明细
     *
     * @param items 明细集合
     * @return 结果
     */
    public int insertPortalOrderItems(List<PortalOrderItem> items);

    /**
     * 按订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单
     */
    public PortalOrder selectPortalOrderByOrderNo(String orderNo);

    /**
     * 按订单号查询明细
     *
     * @param orderNo 订单号
     * @return 明细集合
     */
    public List<PortalOrderItem> selectPortalOrderItemsByOrderNo(String orderNo);

    /**
     * 门户订单查询（手机号/订单号过滤，新单在前）
     *
     * @param order 过滤条件（customerPhone/orderNo）
     * @return 订单集合
     */
    public List<PortalOrder> selectPortalOrderList(PortalOrder order);

    /**
     * 管理端订单列表（配合 PageHelper 分页）
     *
     * @param query 过滤条件（orderNo 模糊/customerPhone/status）
     * @return 订单集合
     */
    public List<PortalOrder> selectOrderList(OrderQueryParam query);

    /**
     * 按主键查询订单（管理端）
     *
     * @param orderId 订单id
     * @return 订单
     */
    public PortalOrder selectOrderById(Long orderId);

    /**
     * 更新订单状态
     *
     * @param orderId 订单id
     * @param status 新状态
     * @return 结果
     */
    public int updateOrderStatus(@Param("orderId") Long orderId, @Param("status") String status);

    /**
     * 原子扣减库存（库存不足时影响行数为0）
     *
     * @param productId 商品id
     * @param quantity 数量
     * @return 影响行数
     */
    public int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 回补库存（取消订单用）
     *
     * @param productId 商品id
     * @param quantity 数量
     * @return 影响行数
     */
    public int restoreStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
