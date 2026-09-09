-- 2026-09-09 功能补齐数据库迁移：后台管理端对齐三级功能清单
-- 用法：mysql -ushixun -p shixun < upgrade-20260909.sql
-- 幂等性说明：整份脚本可安全重复执行（新表 IF NOT EXISTS，加列前自动判断列是否存在）

USE shixun;

-- 1. 广告位上下线字段（P4）：列已存在时自动跳过
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'shixun' AND TABLE_NAME = 'ad_slot' AND COLUMN_NAME = 'online');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `ad_slot` ADD COLUMN `online` bit(1) DEFAULT b''1''',
    'SELECT ''ad_slot.online 已存在，跳过''');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE `ad_slot` SET `online` = b'1' WHERE `online` IS NULL;

-- 2. C 端订单（P1）
CREATE TABLE IF NOT EXISTS `customer_order` (
    `id`           bigint       NOT NULL AUTO_INCREMENT,
    `order_no`     varchar(32)  DEFAULT NULL,
    `customer`     varchar(64)  DEFAULT NULL,
    `order_type`   varchar(32)  DEFAULT NULL,
    `amount`       varchar(32)  DEFAULT NULL,
    `order_status` varchar(16)  DEFAULT NULL,
    `created`      varchar(32)  DEFAULT NULL,
    `note`         varchar(128) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 3. 商户周期结算（P1）
CREATE TABLE IF NOT EXISTS `settlement_record` (
    `id`            bigint      NOT NULL AUTO_INCREMENT,
    `merchant`      varchar(64) DEFAULT NULL,
    `period`        varchar(16) DEFAULT NULL,
    `orders`        int         NOT NULL,
    `amount`        varchar(32) DEFAULT NULL,
    `commission`    varchar(32) DEFAULT NULL,
    `settle_status` varchar(16) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 4. B 端发票申请（P1）
CREATE TABLE IF NOT EXISTS `invoice_apply` (
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `apply_no`       varchar(32)  DEFAULT NULL,
    `applicant`      varchar(64)  DEFAULT NULL,
    `invoice_title`  varchar(128) DEFAULT NULL,
    `tax_no`         varchar(32)  DEFAULT NULL,
    `amount`         varchar(32)  DEFAULT NULL,
    `invoice_status` varchar(16)  DEFAULT NULL,
    `applied`        varchar(32)  DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 5. C 端用户（P2）
CREATE TABLE IF NOT EXISTS `app_user` (
    `id`          bigint      NOT NULL AUTO_INCREMENT,
    `account`     varchar(64) DEFAULT NULL,
    `reg_source`  varchar(16) DEFAULT NULL,
    `membership`  varchar(16) DEFAULT NULL,
    `registered`  varchar(32) DEFAULT NULL,
    `last_active` varchar(32) DEFAULT NULL,
    `user_status` varchar(16) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 6. 语种包 / 课程知识包审核（P3）
CREATE TABLE IF NOT EXISTS `audit_package` (
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `package_type`   varchar(16)  DEFAULT NULL,
    `package_name`   varchar(128) DEFAULT NULL,
    `source`         varchar(64)  DEFAULT NULL,
    `meta`           varchar(128) DEFAULT NULL,
    `package_status` varchar(16)  DEFAULT NULL,
    `sort_order`     int          NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 7. 系统运行日志（P5）
CREATE TABLE IF NOT EXISTS `system_log` (
    `id`         bigint       NOT NULL AUTO_INCREMENT,
    `log_time`   varchar(32)  DEFAULT NULL,
    `log_level`  varchar(8)   DEFAULT NULL,
    `category`   varchar(16)  DEFAULT NULL,
    `source`     varchar(64)  DEFAULT NULL,
    `message`    varchar(255) DEFAULT NULL,
    `sort_order` int          NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 8. 熔断降级策略（P5）
CREATE TABLE IF NOT EXISTS `circuit_breaker` (
    `id`            bigint      NOT NULL AUTO_INCREMENT,
    `service_name`  varchar(64) DEFAULT NULL,
    `strategy`      varchar(32) DEFAULT NULL,
    `threshold`     varchar(64) DEFAULT NULL,
    `breaker_state` varchar(16) DEFAULT NULL,
    `enabled`       bit(1)      DEFAULT NULL,
    `sort_order`    int         NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 9. 备份策略（P5）
CREATE TABLE IF NOT EXISTS `backup_policy` (
    `id`         bigint      NOT NULL AUTO_INCREMENT,
    `target`     varchar(64) DEFAULT NULL,
    `cycle`      varchar(16) DEFAULT NULL,
    `retention`  varchar(32) DEFAULT NULL,
    `storage`    varchar(64) DEFAULT NULL,
    `enabled`    bit(1)      DEFAULT NULL,
    `sort_order` int         NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
