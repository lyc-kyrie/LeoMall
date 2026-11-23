drop database if exists leomall;
drop user if exists 'leomall'@'%';
-- 支持emoji：需要mysql数据库参数： character_set_server=utf8mb4
create database leomall default character set utf8mb4 collate utf8mb4_unicode_ci;
use leomall;
create user 'leomall'@'%' identified by 'leomall123456';
grant all privileges on leomall.* to 'leomall'@'%';
flush privileges;