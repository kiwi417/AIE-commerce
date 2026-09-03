-- ==============================================
-- 商城订单表（门户下单 + 管理端订单管理）
-- 执行前确认已选择业务库 ry
-- 下单扣库存、取消回补库存；状态：pending待取货 / confirmed已确认 / completed已完成 / cancelled已取消
-- ==============================================

drop table if exists mall_order;
create table mall_order (
  order_id       bigint(20)     not null auto_increment comment '订单id',
  order_no       varchar(32)    not null                comment '订单号',
  session_id     varchar(64)    not null                comment '会话id（门户匿名会话）',
  customer_phone varchar(20)    not null                comment '取货手机号',
  customer_name  varchar(50)    default ''              comment '取货人姓名',
  total_amount   decimal(10,2)  not null                comment '订单总金额',
  item_count     int(11)        not null default 0      comment '商品件数',
  status         varchar(20)    not null default 'pending' comment '状态（pending/confirmed/completed/cancelled）',
  create_by      varchar(64)    default ''              comment '创建者',
  create_time    datetime                               comment '创建时间',
  update_by      varchar(64)    default ''              comment '更新者',
  update_time    datetime                               comment '更新时间',
  remark         varchar(255)   default null            comment '备注',
  primary key (order_id),
  unique key uk_order_no (order_no),
  key idx_session (session_id),
  key idx_phone (customer_phone)
) engine=innodb auto_increment=1 default charset=utf8mb4 comment='商城订单表';

drop table if exists mall_order_item;
create table mall_order_item (
  item_id      bigint(20)     not null auto_increment comment '明细id',
  order_no     varchar(32)    not null                comment '订单号',
  product_id   bigint(20)     not null                comment '商品id',
  product_name varchar(200)   default ''              comment '商品名称（下单时快照）',
  price        decimal(10,2)  not null                comment '成交单价（快照）',
  quantity     int(11)        not null                comment '购买数量',
  subtotal     decimal(10,2)  not null                comment '小计',
  shelf_area   varchar(50)    default ''              comment '货架位置（快照）',
  unit         varchar(20)    default ''              comment '单位（快照）',
  primary key (item_id),
  key idx_order_no (order_no),
  key idx_product (product_id)
) engine=innodb auto_increment=1 default charset=utf8mb4 comment='商城订单明细表';

-- ==============================================
-- 订单管理菜单（挂在「商城管理」目录 menu_id=2000 下，与 mall_admin.sql 风格一致）
-- C 菜单渲染侧边栏入口，F 菜单提供按钮权限字符串
-- ==============================================

insert into sys_menu values('2011', '订单管理', '2000', '3', 'order', 'business/order/index', '', '', 1, 0, 'C', '0', '0', 'business:order:list', 'form', 'admin', sysdate(), '', null, '订单管理菜单');
insert into sys_menu values('2012', '订单查询', '2011', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'business:order:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2013', '订单完成', '2011', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'business:order:complete', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2014', '订单取消', '2011', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'business:order:cancel', '#', 'admin', sysdate(), '', null, '');

