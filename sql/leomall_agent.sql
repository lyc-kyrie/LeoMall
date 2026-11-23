-- =============================================================
-- leomall 智能导购 Agent 会话记录表
-- 用途：用户与 Agent 的对话、消息与推荐商品落库，供管理后台查询。
-- 幂等：可重复执行（会 DROP 并重建这三张表，不影响其他业务表）。
-- 全量安装时，本文件里的 CREATE TABLE 也已同步追加到 leomall_table.sql。
-- =============================================================

USE `leomall`;

DROP TABLE IF EXISTS `leomall_agent_recommendation`;
DROP TABLE IF EXISTS `leomall_agent_message`;
DROP TABLE IF EXISTS `leomall_agent_session`;

CREATE TABLE `leomall_agent_session` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL COMMENT '用户表的用户ID',
  `session_id` varchar(63) NOT NULL COMMENT '会话业务ID，与 Agent 服务 shopping_session_id 一致',
  `title` varchar(127) DEFAULT '' COMMENT '会话标题，取首问截断',
  `turn_count` int(11) DEFAULT '0' COMMENT '对话轮数（一问一答算一轮）',
  `last_query` varchar(255) DEFAULT '' COMMENT '最近一次提问',
  `status` int(11) DEFAULT '0' COMMENT '状态：0 正常，1 用户已清空',
  `model` varchar(63) DEFAULT '' COMMENT '最近一次使用的模型',
  `add_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `agent_session_uk` (`session_id`),
  KEY `agent_session_user` (`user_id`),
  KEY `agent_session_add_time` (`add_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能导购会话表';

CREATE TABLE `leomall_agent_message` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(63) NOT NULL COMMENT '会话业务ID',
  `user_id` int(11) NOT NULL COMMENT '用户表的用户ID',
  `role` varchar(16) NOT NULL DEFAULT 'user' COMMENT '角色：user / assistant',
  `content` varchar(2048) DEFAULT '' COMMENT '消息正文',
  `model` varchar(63) DEFAULT '' COMMENT '生成该回复的模型，user 消息为空',
  `latency_ms` int(11) DEFAULT '0' COMMENT '本轮耗时（毫秒）',
  `degraded` tinyint(1) DEFAULT '0' COMMENT '是否降级推荐（无 LLM 密钥时为 1）',
  `add_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `agent_message_session` (`session_id`),
  KEY `agent_message_user` (`user_id`),
  KEY `agent_message_add_time` (`add_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能导购消息表';

CREATE TABLE `leomall_agent_recommendation` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `message_id` int(11) NOT NULL DEFAULT '0' COMMENT '关联的助手消息ID',
  `session_id` varchar(63) NOT NULL COMMENT '会话业务ID',
  `user_id` int(11) NOT NULL COMMENT '用户表的用户ID',
  `goods_id` int(11) NOT NULL COMMENT 'leomall_goods.id，BFF 已校验在上售',
  `goods_name` varchar(127) DEFAULT '' COMMENT '推荐时的商品名称快照',
  `goods_pic` varchar(255) DEFAULT '' COMMENT '推荐时的商品主图快照',
  `retail_price` decimal(10,2) DEFAULT '0.00' COMMENT '推荐时的零售价快照',
  `score` decimal(10,4) DEFAULT '0.0000' COMMENT '召回/精排分数',
  `sort_rank` int(11) DEFAULT '0' COMMENT '推荐位次（rank 是 MySQL 8 保留字）',
  `clicked` tinyint(1) DEFAULT '0' COMMENT '用户是否点击了商品卡',
  `add_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `agent_rec_message` (`message_id`),
  KEY `agent_rec_session` (`session_id`),
  KEY `agent_rec_goods` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能导购推荐记录表';
