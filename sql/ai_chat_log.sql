-- ----------------------------
-- 1、AI客服对话日志表
-- ----------------------------
drop table if exists ai_chat_log;
create table ai_chat_log (
  log_id       bigint(20)     not null auto_increment comment '日志id',
  session_id   varchar(64)    default ''                 comment '会话id',
  question     varchar(1000)  default ''                 comment '用户问题',
  answer       text                                      comment 'AI回答（流式中断时存部分回答）',
  intent       varchar(32)    default ''                 comment '意图 CONSULTATION咨询/ANALYTICS分析/OPERATION操作',
  agent        varchar(32)    default ''                 comment 'Agent（本阶段与意图同值）',
  latency_ms   int(11)        default 0                  comment '耗时（毫秒，Java侧测量）',
  create_by    varchar(64)    default null               comment '创建者（门户匿名为空）',
  create_time  datetime                                  comment '创建时间',
  primary key (log_id),
  key idx_session (session_id),
  key idx_intent (intent),
  key idx_create_time (create_time)
) engine=innodb auto_increment=1 default charset=utf8mb4 comment = 'AI客服对话日志表';
