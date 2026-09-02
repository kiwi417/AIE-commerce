-- ----------------------------------------------------------------------------
-- 商城后台管理：菜单 + 普通员工只读授权 + 演示员工账号
--
-- 权限模型：
--   mall:product:list / query / add / edit / remove
--   mall:category:list / query / add / edit / remove
-- 普通员工（role 100）只授予 list / query（只读：能看库存和商品分类，不能增删改）；
-- 超级管理员（role 1）不受菜单权限限制，可全部操作。
-- ----------------------------------------------------------------------------

-- 1. 菜单（menu_id 从 2000 起，避开现有 1060 上限）
insert into sys_menu values('2000', '商城管理', '0', '4', 'mall', null, '', '', 1, 0, 'M', '0', '0', '', 'shopping', 'admin', sysdate(), '', null, '商城管理目录');
insert into sys_menu values('2001', '商品管理', '2000', '1', 'product', 'mall/product/index', '', '', 1, 0, 'C', '0', '0', 'mall:product:list', 'shopping', 'admin', sysdate(), '', null, '商品管理菜单');
insert into sys_menu values('2002', '分类管理', '2000', '2', 'category', 'mall/category/index', '', '', 1, 0, 'C', '0', '0', 'mall:category:list', 'list', 'admin', sysdate(), '', null, '商品分类管理菜单');
-- 商品按钮权限
insert into sys_menu values('2003', '商品查询', '2001', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'mall:product:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2004', '商品新增', '2001', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'mall:product:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2005', '商品修改', '2001', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'mall:product:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2006', '商品删除', '2001', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'mall:product:remove', '#', 'admin', sysdate(), '', null, '');
-- 分类按钮权限
insert into sys_menu values('2007', '分类查询', '2002', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'mall:category:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2008', '分类新增', '2002', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'mall:category:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2009', '分类修改', '2002', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'mall:category:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2010', '分类删除', '2002', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'mall:category:remove', '#', 'admin', sysdate(), '', null, '');

-- 2. 普通员工（role 100）只读授权：目录 + 两个菜单 + 查询按钮（无 add/edit/remove）
insert into sys_role_menu values ('100', '2000');
insert into sys_role_menu values ('100', '2001');
insert into sys_role_menu values ('100', '2003');
insert into sys_role_menu values ('100', '2002');
insert into sys_role_menu values ('100', '2007');

-- 3. 演示员工账号 zhangsan / admin123（普通员工角色，只读验证用）
insert into sys_user(user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar,
                     password, status, del_flag, login_ip, login_date, create_by, create_time, update_by, update_time, remark)
values (3, 103, 'zhangsan', '张三', '00', 'zhangsan@163.com', '15900000001', '1', '',
        '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), 'admin', sysdate(), '', null, '演示员工（只读权限）');
insert into sys_user_role values ('3', '100');
