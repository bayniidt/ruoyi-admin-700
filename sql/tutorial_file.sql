-- 广告账户注册及其他教学

create table if not exists tutorial_file (
  file_id bigint(20) not null auto_increment comment '文件ID',
  file_name varchar(255) not null comment '文件名称',
  file_path varchar(500) not null comment '文件路径',
  file_type varchar(50) default null comment '文件类型',
  file_size bigint(20) default 0 comment '文件大小',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime comment '更新时间',
  remark varchar(500) default null comment '备注',
  primary key (file_id)
) engine=innodb auto_increment=1 comment = '教学文件表';

insert ignore into sys_menu values
(2006, '广告账户注册及其他教学', 0, 3, 'tutorial', 'tutorial/index', '', '', 1, 0, 'C', '0', '0', 'tutorial:file:list', 'education', 'admin', sysdate(), '', null, '广告账户注册及其他教学');

insert ignore into sys_menu values
(2007, '教学文件查询', 2006, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'tutorial:file:list', '#', 'admin', sysdate(), '', null, ''),
(2008, '教学文件新增', 2006, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'tutorial:file:add', '#', 'admin', sysdate(), '', null, ''),
(2009, '教学文件删除', 2006, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'tutorial:file:remove', '#', 'admin', sysdate(), '', null, '');

insert ignore into sys_role_menu(role_id, menu_id)
select role_id, 2006 from sys_role;

insert ignore into sys_role_menu(role_id, menu_id)
select role_id, 2007 from sys_role;

insert ignore into sys_role_menu(role_id, menu_id)
select role_id, menu_id
from sys_role
join sys_menu on menu_id in (2008, 2009)
where role_id = 1;
