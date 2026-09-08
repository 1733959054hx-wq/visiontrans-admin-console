-- =============================================================================
--  视界译 VisionTrans · Admin Console —— 数据库初始化脚本
--  适用：数据库已存在（库名 shixun），只需字符集校正 + 账号授权。
--  用法（用有权限的账号执行，例如 root）：
--     mysql -u root -p < D:\Java_code\adminConsole\adminConsole\db\init.sql
--  或在 Navicat / DataGrip 中打开本文件全选执行。
--
--  说明：建表由 JPA（ddl-auto=update）在应用启动时自动完成，
--        演示数据由 DataInitializer 自动灌入，无需手工 INSERT。
-- =============================================================================

-- 1. 确保已有库 shixun 使用 utf8mb4（完整支持中文与 emoji）
ALTER DATABASE shixun
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. 创建业务账号（如已存在则跳过）
CREATE USER IF NOT EXISTS 'shixun'@'localhost' IDENTIFIED BY '123456';
CREATE USER IF NOT EXISTS 'shixun'@'%' IDENTIFIED BY '123456';

-- 3. 授予 shixun 库全部权限（若只要本机访问，可删掉 '%' 那行）
GRANT ALL PRIVILEGES ON shixun.* TO 'shixun'@'localhost';
GRANT ALL PRIVILEGES ON shixun.* TO 'shixun'@'%';

FLUSH PRIVILEGES;

-- 4. 验证
SHOW DATABASES LIKE 'shixun';
SHOW GRANTS FOR 'shixun'@'localhost';
