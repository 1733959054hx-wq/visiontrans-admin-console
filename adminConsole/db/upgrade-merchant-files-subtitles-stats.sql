-- =============================================================================
--  upgrade-merchant-files-subtitles-stats.sql
--  商户模块迭代：真实文件上传 + 字幕独立表 + 视频日播放统计
--  幂等：可安全重复执行（列/表存在即跳过）
--  适用：以 validate 模式启动（未开启 ADMIN_DDL_AUTO=update）的环境；
--        update 模式下 Hibernate 会自动建表加列，本脚本作为显式迁移留档。
--  日期：2026-09-11
-- =============================================================================

USE shixun;

-- 1. merchant_subtitle：视频字幕（按视频逐语种维护 SRT/VTT）
CREATE TABLE IF NOT EXISTS merchant_subtitle (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    video_id     BIGINT       NOT NULL,
    lang         VARCHAR(32)  NULL,
    file_name    VARCHAR(128) NULL,
    file_url     VARCHAR(255) NULL,
    sub_format   VARCHAR(8)   NULL,
    uploaded_at  VARCHAR(20)  NULL,
    PRIMARY KEY (id),
    KEY idx_subtitle_video (video_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '商户视频字幕(逐语种)';

-- 2. merchant_video_daily：视频日播放统计（视频 × 日期）
CREATE TABLE IF NOT EXISTS merchant_video_daily (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    video_id    BIGINT        NOT NULL,
    stat_date   VARCHAR(10)   NOT NULL,
    region      VARCHAR(32)   NULL,
    plays       BIGINT        NULL,
    watch_sec   BIGINT        NULL,
    finish_rate DECIMAL(5, 2) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_video_daily (video_id, stat_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '商户视频日播放统计';

-- 3. merchant_material：上传文件地址与原始名（条件加列，幂等）
SET @ddl = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_material' AND COLUMN_NAME = 'file_url') = 0,
    'ALTER TABLE merchant_material ADD COLUMN file_url VARCHAR(255) NULL COMMENT ''落盘文件地址'' AFTER m_status',
    'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_material' AND COLUMN_NAME = 'file_name') = 0,
    'ALTER TABLE merchant_material ADD COLUMN file_name VARCHAR(128) NULL COMMENT ''上传原始文件名'' AFTER file_url',
    'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. merchant_video：上传文件地址与原始名（条件加列，幂等）
SET @ddl = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_video' AND COLUMN_NAME = 'file_url') = 0,
    'ALTER TABLE merchant_video ADD COLUMN file_url VARCHAR(255) NULL COMMENT ''落盘文件地址'' AFTER subtitle_langs',
    'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_video' AND COLUMN_NAME = 'file_name') = 0,
    'ALTER TABLE merchant_video ADD COLUMN file_name VARCHAR(128) NULL COMMENT ''上传原始文件名'' AFTER file_url',
    'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
