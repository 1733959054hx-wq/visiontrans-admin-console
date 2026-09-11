-- =============================================================================
--  upgrade-merchant-session-mobile.sql
--  商户会话多端互通改造：会话落库 + 多设备并存 + device 标识
--  幂等：可安全重复执行
--  日期：2026-09-11
-- =============================================================================

USE shixun;

-- merchant_session.device：登录设备标识（web / android / ios），多端会话并存
SET @ddl = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_session' AND COLUMN_NAME = 'device') = 0,
    'ALTER TABLE merchant_session ADD COLUMN device VARCHAR(32) NULL COMMENT ''登录设备(web/android/ios)'' AFTER role_name',
    'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
