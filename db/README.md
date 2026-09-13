# 视界译 shixun 数据库 · 导入说明

**本文件（`shixun.sql`）是全量可运行快照**：包含建库语句 + 52 张表结构 + 演示数据，
一条命令即可恢复整个数据库。与 `adminConsole/db/shixun.sql` 内容完全一致（每次结构变更两处同步更新）。

## 导入（MySQL 8.0.x，一条命令）

```bash
mysql -uroot -p < shixun.sql
```

或在 Navicat / Workbench / WPS 数据库里"运行 SQL 文件"导入。

⚠️ **导入会先删除并重建 `shixun` 库**（文件内含 DROP/CREATE DATABASE），会覆盖本地同名库——
如果你本地 shixun 库有重要数据请先备份。

## 导入后信息

| 项 | 值 |
|---|---|
| 库名 | `shixun`（utf8mb4） |
| 应用连接 | `jdbc:mysql://localhost:3306/shixun`，业务账号 `shixun / 123456`（仅授权本库） |
| 商户演示账号 | `merchant / merchant123` |
| 管理员演示账号 | `admin / admin123` |

## 与代码的关系

- 表结构由 JPA(Hibernate) 按实体自动建表维护（启动环境变量 `ADMIN_DDL_AUTO=update` 时会自动补建新表/新列），
  本快照用于**初始化/恢复/新成员快速上手**；
- 结构增量迁移脚本在 `adminConsole/db/upgrade-*.sql`（幂等，可重复执行）；
- 每次表结构变更后，请重新导出并**同步更新两处**：
  `adminConsole/db/shixun.sql` 与根目录 `db/shixun.sql`。
