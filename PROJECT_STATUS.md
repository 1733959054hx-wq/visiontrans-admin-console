# PROJECT_STATUS.md — 项目现状与开发指南

> 本文档是给 AI 助手新会话 / 新成员准备的**项目状态快照**。
> 读完本文即可无缝接手全部工作，无需翻阅历史对话。
> 最后更新：2026-09-09 · 提交 13ab547

---

## 一、项目概览

**视界译 VisionTrans** — 基于手机摄像头的 VR 实时解析和翻译系统
- **团队**：贵州慧科未来科技有限公司 · 实训项目
- **仓库**：
  - `origin` → https://github.com/jcJC123jcJC/visiontrans-merchant.git（金辰私有仓库）
  - `upstream` → https://github.com/1733959054hx-wq/visiontrans-admin-console.git（队友公共仓库）
  - **协作约定**：以队友仓库 main 为唯一主线；你改完 → `git push` + `git push upstream main`；他推了 → `git pull upstream main`
- **本地路径**：`D:\shixun`（唯一活跃副本，旧副本 `D:\慧科实习文件\VR项目代码\visiontrans-admin-console` 已停用勿改）

## 二、技术栈与环境

| 组件 | 版本 / 路径 |
|---|---|
| JDK | **21**（`C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot`） |
| Maven | 3.9.16（`C:\Users\pc\tools\apache-maven-3.9.16\bin\mvn.cmd`） |
| Node.js | v22.14.0（D:\nvm\v22.14.0） |
| MySQL | 8.0.41（localhost:3306，root 密码见下方） |
| 后端端口 | 8080 |
| 前端端口 | 5173（Vite dev，`/api` 代理到 8080） |

### 后端环境变量（IDEA 启动配置必填）
```
ADMIN_DB_USER=shixun
ADMIN_DB_PASSWORD=123456
ADMIN_DDL_AUTO=update
```

### 数据库
- 库名 `shixun`（utf8mb4），50 张表
- 快照文件：`adminConsole/db/shixun.sql`（全量含数据，导入即恢复）
- 迁移脚本：`adminConsole/db/upgrade-*.sql`（幂等，可安全重复执行）
- root 密码：`jcJC198101`
- 业务账号：`shixun / 123456`（GRANT ALL ON shixun.*）

## 三、架构与分层

**技术栈**：Spring Boot 4.1.1 + JPA(Hibernate) + Vue 3 + Vite + Pinia + Tailwind CSS + ECharts

### MVVM 分层
| 层 | 后端 | 前端 |
|---|---|---|
| View | `controller/` | `views/*.vue` + `components/*.vue` |
| ViewModel | `service/` + `dto/` | `stores/` (Pinia) |
| Model | `entity/` + `repository/` + `model/` | `api/*.js` (axios) |

### 关键约定
- **统一响应**：`{ code: 200, message: "OK", data: {...}, timestamp: ... }`，业务失败 `code != 200`
- **鉴权**：请求头 `X-Auth-Token`；商户令牌与管理员令牌**双向隔离**（互调返回 403）
- **写操作**：必须显式声明 `@RequireRole`，未声明的写方法被拦截器按最小权限拒绝
- **角色编码**：`SUPER_ADMIN` / `OPERATIONS` / `AUDITOR` / `MERCHANT`
- **商户模块**：代码全部在 `jingchen` 包 / 目录内，**不改基础工程任何原有文件**
- **前端模块**：代码全部在 `admin-front/src/jingchen/` 目录内
- **通用组件**：CrudDialog / ConfirmDialog / KpiCard / PageHeader / StatusPill / ToggleSwitch 等，新页面直接复用
- **CSS 类**：card / card-h / card-t / card-s / card-b / btn / btn-primary / btn-ghost / btn-sm / pill / pill-green / pill-blue / pill-amber / pill-red / pill-slate / field / lbl / bar / rng / num / seg

## 四、目录结构（活跃代码）

```
D:\shixun\
├── adminConsole/                    # 后端 Spring Boot
│   └── src/main/java/com/gzu/adminconsole/
│       ├── config/                  # AuthInterceptor / RequireRole / DataInitializer / WebConfig
│       ├── common/                  # Result / ResultCode / BusinessException / PasswordHasher / RsaKeyHolder
│       ├── controller/              # 主工程控制器(a6~a11)
│       ├── entity/                  # 主工程实体(22+张表)
│       ├── repository/              # EntityManager + JPQL
│       ├── service/                 # 业务服务
│       ├── dto/                     # 视图对象(按模块分子包)
│       └── jingchen/                # ★ 商户模块（金辰负责）
│           ├── common/MerchantConstants.java    # ROLE_CODE=MERCHANT / API_PREFIX=/merchant
│           ├── config/JingchenProperties.java   # 模块自有配置
│           ├── config/MerchantAccountInitializer.java
│           ├── config/MerchantPlanDataInitializer.java
│           ├── config/MerchantStatDataInitializer.java
│           ├── config/MerchantOrderDataInitializer.java
│           ├── config/MerchantMaterialDataInitializer.java
│           ├── config/MerchantVideoDataInitializer.java
│           ├── config/MerchantPromoDataInitializer.java
│           ├── config/MerchantFundsDataInitializer.java
│           ├── config/MerchantGoodsDataInitializer.java
│           ├── controller/MerchantAuthController.java     # 登录/登出/me
│           ├── controller/MerchantController.java         # 工作台首页
│           ├── controller/MerchantPlanController.java     # 投放计划 CRUD + slots
│           ├── controller/MerchantOverviewController.java # 经营概览
│           ├── controller/MerchantOrderController.java    # 订单结算
│           ├── controller/MerchantMaterialController.java # 素材 CRUD + AB
│           ├── controller/MerchantVideoController.java    # 视频 CRUD
│           ├── controller/MerchantPromoController.java    # 渠道 CRUD
│           ├── controller/MerchantSalesController.java    # 销售报表
│           ├── controller/MerchantFundsController.java    # 资金充值/提现
│           ├── controller/MerchantGoodsController.java    # 商品 CRUD + 上下架
│           ├── controller/MerchantOnboardController.java  # 入驻提交/签合同
│           ├── controller/MerchantProfileController.java  # 资料维护
│           ├── entity/MerchantEntity.java                 # 商户账号(merchant_account)
│           ├── entity/MerchantSessionEntity.java          # 商户会话(merchant_session)
│           ├── entity/MerchantOnboardingEntity.java       # → 主包也有入驻实体
│           ├── entity/MerchantPlanEntity.java             # 投放计划(ad_plan)
│           ├── entity/MerchantDailyStatEntity.java        # 经营日统计(merchant_daily_stat)
│           ├── entity/MerchantOrderEntity.java            # 订单(merchant_order)
│           ├── entity/MerchantMaterialEntity.java         # 素材(merchant_material)
│           ├── entity/MerchantAbConfigEntity.java         # AB配置(merchant_ab_config)
│           ├── entity/MerchantVideoEntity.java            # 视频(merchant_video)
│           ├── entity/MerchantChannelEntity.java          # 渠道(merchant_channel)
│           ├── entity/MerchantGoodsEntity.java            # 商品(merchant_goods)
│           ├── entity/MerchantFundFlowEntity.java         # 流水(merchant_fund_flow)
│           ├── entity/MerchantWithdrawEntity.java         # 提现(merchant_withdraw)
│           ├── entity/MerchantProfileExtEntity.java       # 资料扩展(merchant_profile_ext)
│           ├── repository/…                                # 各实体的 EntityManager+JPQL
│           ├── service/…                                   # 业务规则层
│           └── dto/…                                       # record 视图对象
│
├── admin-front/                     # 前端 Vue 3
│   ├── src/jingchen/                # ★ 商户模块（前端）
│   │   ├── api/merchant.js          # 全部商户接口调用
│   │   ├── stores/merchant.js       # 商户工作台状态(九模块)
│   │   ├── router/index.js          # 路由: /merchant /merchant/plans|overview|orders|materials|videos|promo|goods|funds|onboard
│   │   ├── config/index.js          # MERCHANT_ROLE / MERCHANT_ROUTE_PREFIX
│   │   ├── components/MerchantWelcomeCard.vue
│   │   └── views/
│   │       ├── MerchantHomeView.vue       # 工作台首页(九卡)
│   │       ├── MerchantPlansView.vue      # 投放计划管理(三步向导+列表+报表导出)
│   │       ├── MerchantOverviewView.vue   # 经营概览
│   │       ├── MerchantOrdersView.vue     # 订单与结算
│   │       ├── MerchantMaterialsView.vue  # 素材管理(含AB)
│   │       ├── MerchantVideosView.vue     # 视频接入
│   │       ├── MerchantPromoView.vue      # 推广与销售报表
│   │       ├── MerchantGoodsView.vue      # 商品管理
│   │       ├── MerchantFundsView.vue      # 账户与资金(含资料维护)
│   │       └── MerchantOnboardView.vue    # 入驻管理(三步状态机)
│   ├── public/
│   ├── src/views/LoginView.vue      # 登录页(管理员/商户身份切换)
│   └── src/router/index.js          # 主路由(注册 merchantRoutes,守卫按角色分发)
│
├── db/shixun.sql                    # 全量库快照(50张,含数据)
├── 商户端模块说明.md
└── 联调说明-商户端接入.md
```

## 五、业务模块完成状态

| # | 模块 | 表 | 接口路由 | 页面路由 | 功能覆盖 |
|---|---|---|---|---|---|
| 1 | 经营概览 | merchant_daily_stat(14) | GET /overview | /merchant/overview | 指标卡+趋势+订单摘要 ✅ |
| 2 | 投放计划 | ad_plan(4) | CRUD+pause/resume+GET /slots | /merchant/plans | 三步向导(广告位/时段/人群)+列表+报表导出 ✅ |
| 3 | 订单与结算 | merchant_order(8) | GET /orders+PUT /{id}/settle | /merchant/orders | 列表+结算流转 ✅ |
| 4 | 素材管理 | merchant_material(4)+merchant_ab_config(1) | CRUD+/ab | /merchant/materials | CRUD+A/B 分流 ✅ |
| 5 | 视频接入 | merchant_video(6) | CRUD | /merchant/videos | 档案/元数据/字幕/播放数据 ✅ |
| 6 | 推广与销售 | merchant_channel(4) | CRUD+/sales | /merchant/promo | 渠道+二维码+销售报表 ✅ |
| 7 | 账户与资金 | merchant_fund_flow(9)+merchant_withdraw(2) | GET /funds+POST /recharge|withdraw | /merchant/funds | 余额+充值提现+流水 ✅ |
| 8 | 商品管理 | merchant_goods(6) | CRUD+shelf/unshelf | /merchant/goods | 上架/下架+折扣定价 ✅ |
| 9 | 入驻管理 | merchant_onboarding(5) | GET mine+POST /submit|contract/sign | /merchant/onboard | 三步状态机 ✅ |
| 10 | 资料维护 | merchant_profile_ext(1)+merchant_account | GET/PUT /profile | 弹窗(资金页) | 联系人/电话/结算账户 ✅ |

## 六、登录账号

| 身份 | 账号 | 口令 | 去向 |
|---|---|---|---|
| 商户 | `merchant` | `merchant123` | /merchant（商户工作台） |
| 管理员 | `admin` | `admin123` | /cluster（平台管理后台） |
| 管理员 | `chenmo` | `admin123` | 同上 |

## 七、商户端模块待完善事项（下一迭代）

| # | 缺口 | 难度 | 说明 |
|---|---|---|---|
| 1 | 真实文件上传 | ⭐⭐⭐ | 素材/视频/资质目前为"演示建档案"，Spring Boot 接 MultipartFile 即可 |
| 2 | 字幕管理独立化 | ⭐⭐ | 字幕目前是视频表的文本字段，建议加 merchant_subtitle 表做关联 CRUD |
| 3 | 视频播放统计页 | ⭐⭐ | 播放趋势/完播率/地域分布专用视图 |
| 4 | 提现审核联动 | ⭐ | 提现状态流转由管理端执行（队友负责侧） |
| 5 | 管理员侧审核入口 | ⭐ | 入驻申请审核页面（队友管理端 SecurityView 已有雏形） |

## 八、协作流程速查

```bash
# 拉取队友最新
git pull upstream main

# 提交推送
git add -A
git commit -m "feat: xxx"
git push                # 推到你的仓库(origin)
git push upstream main  # 同步给队友(upstream)

# 遇到队友推送比你多
git pull upstream main   # 快进或合并
```

## 九、注意事项

1. **会话存内存**：管理员/商户会话均为内存态（重启即注销），需要重新登录
2. **不要同时跑两份后端**：8080 端口唯一
3. **旧副本勿启动**：`D:\慧科实习文件\VR项目代码\visiontrans-api` 是废弃的独立工程（占用 8080 且连 visiontrans 库）
4. **`shixun.sql` 应与实际库保持一致**：每次新加表/列后重新导出 mysqldump 全量覆盖
5. **前端热更新**：Vite dev 下改动 .vue 文件即时生效，无需重启
6. **密码哈希**：PasswordHasher 用 PBKDF2(600k 迭代)，用 `PasswordHasher.hash(明文)` 生成，登录时 `matches(明文, hash)` 校验
