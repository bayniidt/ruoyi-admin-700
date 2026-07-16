-- 下级代理开放接口与消耗统计（MySQL 8+）
-- 部署时执行一次；接口密钥只保存 BCrypt 散列，明文仅在创建/重置时返回一次。

create table if not exists agent_client (
  agent_id           bigint(20)      not null auto_increment comment '代理ID',
  owner_user_id      bigint(20)      not null                comment '上级系统用户ID',
  sys_user_id        bigint(20)      not null                comment '若依登录用户ID',
  user_name          varchar(30)     not null                comment '代理用户名',
  password_hash      varchar(100)    not null                comment '代理登录密码散列（预留）',
  nick_name          varchar(30)     not null                comment '代理昵称',
  phonenumber        varchar(20)     default ''              comment '手机号码',
  email              varchar(100)    default ''              comment '邮箱',
  commission_rate    decimal(5,2)    not null default 0      comment '佣金比例',
  partner_customer_key varchar(100)  not null                comment 'PartnerStack客户Key',
  api_key             varchar(80)     not null                comment '开放接口Key',
  api_secret_hash     varchar(100)    not null                comment '开放接口Secret散列',
  status              char(1)         not null default '0'    comment '状态（0正常 1停用）',
  remark              varchar(500)    default null            comment '备注',
  create_by           varchar(64)     default ''              comment '创建者',
  create_time         datetime                                comment '创建时间',
  update_by           varchar(64)     default ''              comment '更新者',
  update_time         datetime                                comment '更新时间',
  primary key (agent_id),
  unique key uk_agent_api_key (api_key),
  unique key uk_agent_sys_user (sys_user_id),
  unique key uk_agent_owner_username (owner_user_id, user_name),
  key idx_agent_owner (owner_user_id)
) engine=innodb comment='下级代理接口账号';

-- 已部署过旧版本时补充登录用户及客户归属字段。
set @ddl = if((select count(*) from information_schema.columns where table_schema = database()
    and table_name = 'agent_client' and column_name = 'sys_user_id') = 0,
    'alter table agent_client add column sys_user_id bigint(20) null comment ''若依登录用户ID'' after owner_user_id',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @ddl = if((select count(*) from information_schema.columns where table_schema = database()
    and table_name = 'agent_client' and column_name = 'partner_customer_key') = 0,
    'alter table agent_client add column partner_customer_key varchar(100) null comment ''PartnerStack客户Key'' after commission_rate',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @ddl = if((select count(*) from information_schema.statistics where table_schema = database()
    and table_name = 'agent_client' and index_name = 'uk_agent_sys_user') = 0,
    'alter table agent_client add unique key uk_agent_sys_user (sys_user_id)',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

-- 代理后台角色：仅授予代理业务菜单，不授予系统管理菜单。
insert into sys_role(role_name, role_key, role_sort, data_scope, menu_check_strictly,
    dept_check_strictly, status, del_flag, create_by, create_time, remark)
select '代理用户', 'agent', 3, '2', 1, 1, '0', '0', 'admin', sysdate(), '下级代理后台登录角色'
where not exists (select 1 from sys_role where role_key = 'agent');

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (2000, 2001, 2002, 2003, 2004, 2005)
where r.role_key = 'agent';

-- 将旧版已创建的代理补成可登录的若依用户，并保留其原密码散列。
insert into sys_user(dept_id, user_name, nick_name, email, phonenumber, password,
    status, del_flag, create_by, create_time, remark)
select coalesce(owner.dept_id, 103), a.user_name, a.nick_name, a.email, a.phonenumber,
    a.password_hash, a.status, '0', a.create_by, a.create_time, a.remark
from agent_client a
left join sys_user owner on owner.user_id = a.owner_user_id
left join sys_user existing on existing.user_name = a.user_name and existing.del_flag = '0'
where a.sys_user_id is null and existing.user_id is null;

update agent_client a
join sys_user u on u.user_name = a.user_name and u.del_flag = '0'
set a.sys_user_id = u.user_id,
    a.partner_customer_key = coalesce(nullif(a.partner_customer_key, ''), a.user_name)
where a.sys_user_id is null;

insert ignore into sys_user_role(user_id, role_id)
select a.sys_user_id, r.role_id
from agent_client a
join sys_role r on r.role_key = 'agent'
where a.sys_user_id is not null;

create table if not exists agent_usage (
  usage_id           bigint(20)      not null auto_increment comment '消耗记录ID',
  agent_id           bigint(20)      not null                comment '代理ID',
  request_id         varchar(64)     not null                comment '调用方幂等请求ID',
  metric             varchar(50)     not null                comment '消耗类型，如 ad_spend/api_call',
  data_count         bigint(20)      not null default 0      comment '消耗数据量',
  spend              decimal(18,4)   not null default 0      comment '消耗金额',
  currency           varchar(10)     not null default 'USD'  comment '币种',
  customer_key       varchar(100)    default null            comment '下级系统内客户标识',
  reported_at        datetime        not null                comment '业务发生时间',
  remark              varchar(500)    default null            comment '备注',
  create_time         datetime                                comment '接收时间',
  primary key (usage_id),
  unique key uk_agent_request (agent_id, request_id),
  key idx_agent_reported (agent_id, reported_at),
  constraint fk_agent_usage_agent foreign key (agent_id) references agent_client (agent_id)
) engine=innodb comment='下级代理消耗上报';
