drop database if exists leomall;
drop user if exists 'leomall'@'%';
-- 支持emoji：需要mysql数据库参数： character_set_server=utf8mb4
create database leomall default character set utf8mb4 collate utf8mb4_unicode_ci;
use leomall;
create user 'leomall'@'%' identified by '123456';
-- P2-12：应用账号最小权限——仅 DML + 备份所需（SHOW VIEW/TRIGGER/LOCK TABLES/EVENT），
-- 不再授予 CREATE/DROP/ALTER 等 DDL；建库建表请用 root 执行本脚本与 table 脚本。
-- 口令 123456 仅为开发默认值，生产必须修改（见 docs/AUDIT.md P1-7）。
grant SELECT, INSERT, UPDATE, DELETE, SHOW VIEW, TRIGGER, LOCK TABLES, EVENT on leomall.* to 'leomall'@'%';
flush privileges;