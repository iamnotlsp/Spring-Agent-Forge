SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `draw_io_agent`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `draw_io_agent`;

CREATE TABLE IF NOT EXISTS `credit_account` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `available_credits` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '可用额度',
  `frozen_credits` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '冻结额度',
  `total_granted_credits` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计发放额度',
  `total_used_credits` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计使用额度',
  `status` varchar(16) NOT NULL DEFAULT 'NORMAL' COMMENT '账户状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='额度账户表';

CREATE TABLE IF NOT EXISTS `credit_order` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `team_id` varchar(32) DEFAULT NULL COMMENT '拼团ID',
  `order_id` varchar(64) DEFAULT NULL COMMENT '拼团服务订单ID',
  `out_trade_no` varchar(64) NOT NULL COMMENT '外部交易单号',
  `goods_id` varchar(64) DEFAULT NULL COMMENT '商品ID',
  `goods_name` varchar(128) DEFAULT NULL COMMENT '商品名称',
  `credits` decimal(18,2) NOT NULL COMMENT '应发放额度',
  `pay_price` decimal(18,2) DEFAULT NULL COMMENT '支付金额',
  `status` varchar(32) NOT NULL COMMENT '订单状态',
  `paid_time` datetime DEFAULT NULL COMMENT '支付时间',
  `grant_time` datetime DEFAULT NULL COMMENT '发放时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_out_trade_no` (`out_trade_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_team_id` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='额度订单表';

CREATE TABLE IF NOT EXISTS `credit_grant_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `team_id` varchar(32) DEFAULT NULL COMMENT '拼团ID，普通购买为空',
  `out_trade_no` varchar(64) NOT NULL COMMENT '外部交易单号',
  `credits` decimal(18,2) NOT NULL COMMENT '发放额度',
  `grant_type` varchar(32) NOT NULL COMMENT '发放类型',
  `grant_status` varchar(32) NOT NULL COMMENT '发放状态',
  `grant_time` datetime NOT NULL COMMENT '发放时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_out_trade_no_grant_type` (`out_trade_no`, `grant_type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_team_id` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='额度发放流水表';

CREATE TABLE IF NOT EXISTS `credit_use_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `request_id` varchar(64) NOT NULL COMMENT '请求ID',
  `agent_id` varchar(64) DEFAULT NULL COMMENT '智能体ID',
  `session_id` varchar(128) DEFAULT NULL COMMENT '会话ID',
  `duration_ms` bigint(20) NOT NULL DEFAULT 0 COMMENT '模型调用耗时毫秒',
  `used_credits` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '消耗额度',
  `use_status` varchar(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '消费状态',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_id` (`request_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='额度消费流水表';

SET FOREIGN_KEY_CHECKS = 1;
