# 视界译 VisionTrans · 平台管理后台（Admin Console）

基于 `demo/管理后台-AdminConsole.html` 高保真原型，落地的一套**前后端分离**管理后台：

- **前端**：Vue 3 + Vite + Pinia + Vue Router + Tailwind CSS + ECharts + Axios
- **后端**：Spring Boot 4.1.1（Java 21，Maven）+ Spring Data JPA（Hibernate）
- **数据库**：MySQL 8.x（库名 `shixun`，账号 `shixun` / `123456`），表结构由 JPA 自动维护，演示数据首次启动自动灌入
- **架构**：MVVM —— 前端 `views(View) ↔ stores(ViewModel) ↔ api(Model 代理)`；后端 `Controller(View) ↔ Service+DTO(ViewModel) ↔ Repository+Entity(Model)`

原型中 5 个页面已 1:1 还原，并且由"写死的假数据"升级为 **REST 接口 + MySQL 落库的真实增删改查**：所有列表都能新增 / 编辑 / 删除，所有按钮都有真实行为，写操作会落库并追加一条不可篡改的审计日志。

## 交互能力一览（每个按钮都有行为）

| 模块 | 可交互项 |
|---|---|
| 集群态势 | 波形图 ⇄ 拓扑视图切换、容量推演、**容器节点增删改查 + 导出 CSV**、告警登记 / 删除、拓扑节点点击直接编辑 |
| AI 模型 | **模型登记 / 编辑 / 删除**、秒级热更、一键回滚、灰度比例滑块、策略开关点击切换、清单导出 CSV |
| 术语库 / 风控 | **术语包任务增删改查 + 看板左右移动**、**素材机审增删改查 + 导出**、UGC 复核 / 放行 / 封禁、按建议退款 |
| 广告排期 | **广告位增删改查 + 导出**、频控滑块拖动即存、AI 建议采纳 |
| 安全风控 | **RBAC 权限三态点击切换 + 保存**、**设备增删改查 / 封禁**、**套餐增删改查**、**管理员增删改查**、安全策略开关、审计日志**真分页** + 导出存证 |
| 全局 | 顶栏搜索过滤当前页表格、铃铛查看最新告警、帮助说明、快捷键 `F` 全屏 / `H` 折叠侧栏 / `1~5` 切模块 / `?` 帮助 |

---

## 一、项目简介

平台管理后台包含 5 大模块（与后端菜单接口 `/api/meta/nav` 一一对应）：

| 路由 | 菜单 | 模块内容 |
|---|---|---|
| `/cluster` (a6) | 集群态势感知 | 4 项核心指标、五层管线延迟波形、QPS 容量推演、可用区并发、16 容器健康明细 |
| `/models` (a7) | AI 模型与热更 | 9 个模型版本灰度状态、灰度比例滑块（可保存）、秒级热更 / 一键回滚、版本时间线 |
| `/moderation` (a8) | 术语库与 UGC 风控 | 审核看板、素材机审置信度、UGC 原位高亮拦截（复核/放行/封禁）、退款仲裁 |
| `/ads` (a9) | 广告位排期引擎 | 12 广告位 × 7 天甘特库存、单用户频控滑块（可保存）、7×7 eCPM 策略矩阵、AI 建议 |
| `/security` (a10) | 安全风控与 RBAC | 3 角色 × 24 权限三态授权树（可编辑保存）、全球登录监控地图、设备封禁、套餐额度、哈希链审计日志 |

## 二、环境要求

| 组件 | 版本要求 | 说明 |
|---|---|---|
| JDK | 17+（推荐 21） | 本机已有 `C:\Program Files\Microsoft\jdk-21.0.4.7-hotspot` |
| Maven | 3.8+ | 本机 3.9.10 可用 |
| Node.js | 18+（推荐 20/22） | 本机 v22.20.0 可用 |
| MySQL | 8.x（**必需**） | 库名 `shixun`，账号 `shixun` / `123456` |

## 三、目录结构

```
adminConsole/
├─ demo/                                # 原始高保真原型（保留作参考）
├─ adminConsole/                        # 后端 Spring Boot
│  ├─ pom.xml
│  ├─ db/init.sql                       # 建库 + 账号授权脚本（用 root 执行一次）
│  └─ src/main/
│     ├─ java/com/gzu/adminconsole/
│     │  ├─ AdminConsoleApplication.java   # 启动类
│     │  ├─ config/                        # AppProperties、WebConfig（CORS）、DataInitializer（首次启动灌演示数据）
│     │  ├─ common/                        # Result 统一响应、ResultCode、BusinessException、全局异常
│     │  ├─ controller/                    # View 层：6 个 REST 控制器
│     │  ├─ service/                       # ViewModel 层：数据组装 + 业务规则
│     │  ├─ dto/                           # 视图模型（VO），按模块分 common/cluster/model/...
│     │  ├─ repository/                    # Model 层：JPA 实现（EntityManager + JPQL）
│     │  ├─ entity/                        # JPA 实体（22 张表）
│     │  ├─ model/                         # 领域记录（record），Service 层只依赖它
│     │  └─ jingchen/                      # 商户模块（独立分层：common/config/controller/service/dto/entity/repository）
│     └─ resources/
│        └─ application.properties         # ★ 全部后端配置集中于此
└─ admin-front/                         # 前端 Vue 3
   ├─ vite.config.js                    # 端口 5173，/api 代理到 8080
   ├─ tailwind.config.js                # 原型色板（navy/abyss/electric/ice/...）
   ├─ .env.development / .env.production
   └─ src/
      ├─ layouts/AdminLayout.vue        # 侧栏 + 顶栏 + 路由出口 + 全局 Toast
      ├─ router/index.js                # 5 个路由（a6~a10）
      ├─ api/                           # Model 代理层：request.js 统一解包 Result
      ├─ stores/                        # ViewModel 层：Pinia（app/cluster/models/moderation/ads/security/ui）
      ├─ composables/useConfirm.js      # 确认框状态（Promise 风格）
      ├─ utils/csv.js                   # 纯前端 CSV 导出
      ├─ components/                    # 通用组件
      │  ├─ CrudDialog.vue              # 通用增删改查弹窗（按 fields 配置渲染表单）
      │  ├─ ConfirmDialog.vue           # 删除二次确认
      │  ├─ EChart / DonutChart / SparkLine / WorldMap   # 图表
      │  └─ KpiCard / StatusPill / ProgressBar / RankList / ToggleSwitch
      ├─ jingchen/                      # 商户模块（平行分层：config/api/stores/components/views/router）
      └─ views/                         # 5 个业务页面
```

## 四、启动步骤

### 0. 初始化数据库（仅第一次）

库 `shixun` 已存在，只需用**有权限的账号**（如 root）执行一次授权脚本：

```powershell
# 方式一：命令行
mysql -u root -p < D:\Java_code\adminConsole\adminConsole\db\init.sql

# 方式二：用 Navicat / DataGrip 打开 adminConsole/db/init.sql 全选执行
```

脚本内容：把 `shixun` 库字符集校正为 utf8mb4、创建 `shixun` 账号并授予该库全部权限。
表结构默认由 `ddl-auto=validate` 校验（生产推荐，应用账号不持有 DDL 权限）；
本地开发设置 `ADMIN_DDL_AUTO=update` 后由 JPA 自动建表，并自动灌入演示数据。

### 1. 启动后端（端口 8080）

```powershell
# PowerShell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-21.0.4.7-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# 数据库账号 / 口令从环境变量注入（不再写进配置文件，避免凭据入库）
$env:ADMIN_DB_URL = 'jdbc:mysql://localhost:3306/shixun?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
$env:ADMIN_DB_USER = 'shixun'
$env:ADMIN_DB_PASSWORD = '123456'
# 本地开发需要 JPA 自动建表时设置；生产保持默认 validate，结构变更走迁移脚本
$env:ADMIN_DDL_AUTO = 'update'

cd D:\Java_code\adminConsole\adminConsole
mvn spring-boot:run
```

首次启动日志中应出现：

```
[初始化] nav_menu 已写入 5 条菜单
[初始化] cluster_node / alarm_event 已写入 8 / 6 条
[初始化] model_release 已写入 9 条
...
Started AdminConsoleApplication in x.xxx seconds
```

验证：浏览器打开 <http://localhost:8080/api/meta/nav>，应返回 JSON。

### 2. 启动前端（端口 5173）

```powershell
cd D:\Java_code\adminConsole\admin-front
npm install        # 首次执行
npm run dev
```

打开 <http://localhost:5173>，左侧菜单即可在 5 个大盘间切换。

> 前端开发模式下 `/api` 由 Vite 代理转发到 `http://localhost:8080`（见 `.env.development`），无跨域问题。

### 3. 生产构建

```powershell
cd D:\Java_code\adminConsole\admin-front
npm run build          # 产物在 admin-front/dist
```

部署方式二选一：

- **同域部署（推荐）**：把 `dist/` 内容拷贝到 Spring Boot 静态资源目录，或由 Nginx 同域名下先托管静态文件、再反代 `/api` 到 8080，此时 `.env.production` 的 `VITE_API_BASE_URL=/api` 不用改。
- **分离部署**：把 `VITE_API_BASE_URL` 改为 `http://<后端地址>/api` 后重新 build。

## 五、后端配置说明（application.properties）

所有可移植参数集中在 `adminConsole/src/main/resources/application.properties`：

| 配置段 | 内容 |
|---|---|
| `server.*` | 端口、编码、Tomcat 线程 |
| `admin-console.api.*` | REST 前缀（默认 `/api`）、接口开关 |
| `admin-console.platform.*` | 平台名称、顶栏/侧栏文案、当前管理员（改配置即可换品牌，无需动代码） |
| `admin-console.cors.*` | 跨域白名单（部署时改成实际前端域名） |
| `admin-console.cluster.*` | SLA 延迟阈值、设计容量 QPS、采样周期 |
| `admin-console.security.*` | 日志留存天数 |
| `admin-console.data.randomize` | 开启后实时指标做轻微随机扰动，模拟推送效果 |
| `admin-console.data.auto-init` | 启动期是否自动灌入演示数据（表非空则跳过） |
| `spring.datasource.*` | MySQL 连接：库名由 `ADMIN_DB_URL` 注入，账号 / 口令由 `ADMIN_DB_USER` / `ADMIN_DB_PASSWORD` 注入（**不入库**） |
| `spring.jpa.*` | Hibernate 行为（ddl-auto 由 `ADMIN_DDL_AUTO` 控制，默认 validate） |

### 角色与接口权限

登录令牌放在请求头 `X-Auth-Token`。所有接口默认需要登录，权限由接口上的 `@RequireRole` 声明：

| 角色编码 | 中文名 | 权限 |
|---|---|---|
| `SUPER_ADMIN` | 超级管理员 | 全部接口，含删除与模型热更 / 回滚 / 灰度 |
| `OPERATIONS` | 运营管理员 | 各模块新增 / 修改与业务动作，不含删除、不含 RBAC 授权 |
| `AUDITOR` | 只读审计员 | 仅 `GET` 大盘数据 |
| `MERCHANT` | 商户用户 | 仅 `/merchant/**` 工作台接口（与后台管理完全隔离；独立账号 / 令牌 / 会话） |

未标注 `@RequireRole` 的写操作（POST / PUT / PATCH / DELETE）会被 `AuthInterceptor` 按最小权限直接拒绝，避免新增接口漏配角色。

### 五-B、商户模块（jingchen）· 基于角色的登录隔离

商户用户（角色 `MERCHANT`）与主工程**平行独立**：拥有自己的 `controller/service/repository/entity/dto/common/config` 分层（包 `com.gzu.adminconsole.jingchen`），商户侧的数据结构与业务逻辑一律定义在模块内，不复用后台管理的 `model` / `entity`。

**账号**

| 角色 | 登录账号 | 口令 | 令牌存储 | 落地页 |
|---|---|---|---|---|
| 管理员 | `admin` | `admin123` | 后台 `auth_session` | `/cluster` |
| 商户 | `merchant` | `merchant123` | 模块自有 `merchant_session` | `/merchant` |

> 商户账号默认由 `MerchantAccountInitializer` 在 `admin-console.jingchen.enabled=true`（默认）时自动灌入；账号 / 口令可经 `application.properties` 的 `admin-console.jingchen.username` / `.password` 覆盖。

**方案：独立登录（选身份）**

登录页提供「管理员 / 商户」身份切换（顶部分段控件）。二者走**完全独立的登录接口与令牌体系**：

- 管理员：`POST /api/auth/login` → 主工程 `auth_session` 令牌
- 商户：`POST /api/merchant/login` → 模块自有 `merchant_session` 令牌

统一鉴权 `AuthInterceptor` 通过 `common/TokenResolver` SPI 依次尝试主工程与会话解析器：管理员令牌由 `AuthRepository` 解析，商户令牌由 `MerchantSessionRepository` 解析，互不干扰。

**双向隔离（核心）**

| 方向 | 机制 |
|---|---|
| 后台接口防商户 | 后台 5 个控制器（Cluster/Model/Moderation/Ad/Security）类上加 `@RequireRole({"SUPER_ADMIN","OPERATIONS","AUDITOR"})`。原实现只有写操作受限、GET 对任何登录用户开放，商户能读到集群 / 安全等管理数据，已修复 |
| 商户接口防管理员 | `MerchantController` / `MerchantAuthController` 标 `@RequireRole("MERCHANT")` |
| 数据表隔离 | 商户账号 / 凭据 / 会话落在 `merchant_account` / `merchant_session`，**不进 `admin_user`**，因此商户不会出现在后台「管理员账号」列表，也不计入管理员总数 KPI |
| 路由隔离 | 前端路由 `meta.roles`：管理员页仅 `ADMIN_ROLES` 可见，商户页仅 `MERCHANT` 可见；越权访问被守卫强制打回各自首页 |

**共享边界（已与主干业务隔离）**

模块与主干的共用点均集中在主工程的少量「标准扩展点」上，不改动主干业务逻辑：

- `common/TokenResolver`：令牌解析 SPI，主工程与各模块各实现一个，由 `AuthInterceptor` 统一调度（开闭原则）。
- 登录页 `/login`：共用同一页面与「点击式验证码 / RSA 加密」设施；仅在提交时按所选身份调不同接口。
- 加密 / 验证码工具：`RsaKeyHolder`、`CaptchaService`、`PasswordHasher` 为主干公共工具（不含业务数据），商户登录直接复用。

## 六、数据持久化说明

- **技术选型**：`spring-boot-starter-data-jpa`（Hibernate）+ `mysql-connector-j`，Repository 层通过 `EntityManager` + JPQL 操作数据库。
- **表结构**：`ddl-auto=update`，启动时自动建表 / 增量更新字段，共 22 张表（见下方）。
- **演示数据**：`config/DataInitializer` 在启动后检查各主表，为空才灌入；**你手工改过的数据不会被覆盖**。
  想重置演示数据：删表后重启，或设 `admin-console.data.auto-init=false` 自己维护数据。
- **写操作全部落库**：灰度比例、模型热更 / 回滚、UGC 处置、频控配置、RBAC 授权、设备封禁、审计日志都会真实写进 MySQL 并追加操作日志。

主要数据表：

| 分类 | 表名 |
|---|---|
| 集群 | `cluster_node`、`alarm_event` |
| 模型 | `model_release`、`release_event`、`strategy_toggle` |
| 审核 | `glossary_task`、`material_asset`、`ugc_record`、`ugc_segment`、`ugc_hit`、`refund_record` |
| 广告 | `ad_slot`、`frequency_cap` |
| 安全 | `security_role`、`perm_group`、`permission_item`、`device_record`、`membership_plan`、`audit_log`（`monitor_point`） |
| 公共 | `nav_menu`、`app_setting`（灰度比例、回滚次数、建议采纳状态等键值配置） |

## 七、REST 接口一览

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/meta/nav` | 侧边导航菜单 |
| GET | `/api/meta/system` | 品牌与系统状态信息 |
**大盘聚合接口**（一次请求返回整页所需数据）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/meta/nav` | 侧边导航菜单 |
| GET | `/api/meta/system` | 品牌与系统状态信息 |
| GET | `/api/cluster/overview` | 集群态势大盘 |
| GET | `/api/models/overview` | 模型热更中心 |
| GET | `/api/moderation/overview` | 审核与 UGC 风控大盘 |
| GET | `/api/ads/overview` | 广告排期大盘 |
| GET | `/api/security/overview?page=1` | 安全风控大盘（审计日志分页） |

**业务动作接口**

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/cluster/capacity-simulation` | 容量推演 |
| PUT | `/api/models/grayscale?ratio=60` | 调整全局灰度比例 |
| POST | `/api/models/hot-update?name=<模型名>` | 秒级热更至全量 |
| POST | `/api/models/rollback?name=<模型名>` | 一键回滚 |
| PUT | `/api/models/strategies?name=<策略>&enabled=true` | 切换灰度策略开关 |
| POST | `/api/moderation/ugc/decision?action=ban\|pass\|review` | UGC 违规处置 |
| POST | `/api/moderation/refund/process` | 按建议执行退款 |
| PATCH | `/api/moderation/tasks/{id}/move?direction=next` | 看板卡片左右移动 |
| PUT | `/api/ads/frequency` | 更新频控项（body: `{name, value}`） |
| POST | `/api/ads/advice/adopt` | 采纳 AI 调优建议 |
| POST | `/api/security/devices/ban?fingerprint=<指纹>` | 封禁异常设备 |
| PUT | `/api/security/permissions` | 保存 RBAC 三态授权 |
| PUT | `/api/security/policies?name=<策略>&enabled=true` | 切换安全策略开关 |

**增删改查接口**（请求体即 `model` 记录 JSON，如 `{"id":"vt-test-01","zone":"华东 1（上海）",...}`）

| 资源 | 新增 | 修改 | 删除 |
|---|---|---|---|
| 容器节点 | `POST /api/cluster/nodes` | `PUT /api/cluster/nodes` | `DELETE /api/cluster/nodes/{id}` |
| 告警事件 | `POST /api/cluster/alarms` | — | `DELETE /api/cluster/alarms/{id}` |
| AI 模型 | `POST /api/models` | `PUT /api/models` | `DELETE /api/models?name=<模型名>` |
| 术语包任务 | `POST /api/moderation/tasks` | `PUT /api/moderation/tasks` | `DELETE /api/moderation/tasks/{id}` |
| 素材机审 | `POST /api/moderation/assets` | `PUT /api/moderation/assets` | `DELETE /api/moderation/assets/{id}` |
| 广告位 | `POST /api/ads/slots` | `PUT /api/ads/slots` | `DELETE /api/ads/slots/{id}` |
| 设备台账 | `POST /api/security/devices` | `PUT /api/security/devices` | `DELETE /api/security/devices/{fingerprint}` |
| 会员套餐 | `POST /api/security/plans` | `PUT /api/security/plans` | `DELETE /api/security/plans/{name}` |
| 管理员 | `POST /api/security/admins` | `PUT /api/security/admins` | `DELETE /api/security/admins/{id}` |

统一响应结构：`{ "code": 200, "message": "OK", "data": {...}, "timestamp": ... }`，业务失败时 `code != 200`，由前端 `src/api/request.js` 统一解包与报错。

## 八、MVVM 分层对照

| 层 | 后端 | 前端 |
|---|---|---|
| **View（视图）** | `controller/`（只做参数接收与 Result 包装） | `views/*.vue` + `components/*.vue`（只做渲染与事件转发） |
| **ViewModel（视图模型）** | `service/`（业务规则）+ `dto/`（页面定制数据结构） | `stores/*.vue`（Pinia：加载、缓存、调用动作、Toast 提示） |
| **Model（模型）** | `model/`（领域记录）+ `repository/`（JPA 数据访问）+ `entity/`（JPA 实体） | `api/*.js`（HTTP 数据源代理） |

**数据流**：用户操作 → Vue 组件 emit → Pinia action → axios → Controller → Service（校验/变更领域模型）→ Repository 写入 MySQL → 返回 VO → Store 更新 → 视图自动重渲染。
