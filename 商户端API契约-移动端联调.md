# 商户端 API 契约 · 移动端联调说明

> 面向移动端（商家 App / 用户端）同学对接商户模块后端接口的单一权威文档。
> 网页端（`admin-front`）与移动端共用同一批接口，字段语义以本文档 + GET 实际返回为准。
> 最后更新：2026-09-11 · 商户模块负责人：金辰

---

## 一、通用约定

| 项 | 约定 |
|---|---|
| 服务地址 | `http://<服务器IP>:8080`（本地联调 `http://localhost:8080`），所有接口以 `/api` 开头 |
| 响应包装 | 统一 `{"code":200,"message":"OK","data":{...},"timestamp":毫秒}`；**判断 `code===200` 为成功**，业务失败 `code != 200` |
| 鉴权 | 登录后所有请求携带请求头 **`X-Auth-Token: <token>`**；商户令牌与管理员令牌双向隔离（商户令牌调管理端接口 403，反之亦然） |
| 口令加密 | 登录口令必须先 `GET /api/auth/public-key` 取 RSA 公钥（X.509 DER 的 Base64），用 **RSA/ECB/PKCS1Padding** 加密后传输 |
| 验证码 | 点选式验证码（图片 + 按顺序点击 3 个汉字坐标）。**成功登录后 10 分钟内同一账号免验证码**（此时登录可不带验证码参数）；未在宽限期且不带验证码 → 返回业务码 **460**，客户端应弹出验证码后重新提交 |
| 多端会话 | 登录入参带 `device`（web / android / ios / ...，缺省 web）。**同一商户多端并存、互不踢**，每商户最多 5 台设备（溢出淘汰最早过期的会话）。会话有效期 8 小时，**落库持久化，后端重启不失效** |
| 登出 | `POST /merchant/logout` 只注销当前令牌，其他设备不受影响 |
| 错误码 | `200` 成功 · `400` 参数/业务错误 · `401` 未登录或登录过期（清除本地令牌重新登录） · `403` 角色无权限 · `404` 资源不存在 · `460` 需要验证码 · `500` 服务内部错误 |

## 二、登录流程（移动端示例）

```
1. GET  /api/auth/public-key
   → data 为公钥 Base64 字符串

2. POST /api/merchant/login
   请求体：{ "username": "merchant",
             "password": "<RSA加密后的Base64>",
             "captchaId": "<验证码挑战id>",     // 宽限期内可传 null
             "captchaClicks": [{"x":120,"y":40}, ...],  // 依序点击的坐标(验证码图 320x150 坐标系)
             "device": "android" }
   → data: { "token": "…-M", "expireAt": "2026-09-12 10:00:00",
             "profile": { "username":"merchant", "name":"示例商户",
                          "roleName":"商户用户", "roleCode":"MERCHANT", "avatar":"商" } }
   ★ code===460 → 先 GET /api/auth/captcha 取 {id,image(base64),hint}，按 hint 顺序点击图片中
     的 3 个汉字（坐标换算到 320x150 原图坐标），再带 captchaId+captchaClicks 重新提交。

3. 后续所有请求头携带 X-Auth-Token: <token>
```

## 三、文件上传与取回

| 接口 | 说明 |
|---|---|
| `POST /api/merchant/files`（multipart） | **需商户令牌**。表单字段：`file`（文件）、`kind`（`image` / `video` / `subtitle` / `doc`）。类型白名单：image jpg/jpeg/png/gif/webp/bmp ≤20MB；video mp4/mov/m4v/webm/avi/mkv ≤500MB；subtitle srt/vtt ≤5MB；doc pdf/jpg/jpeg/png ≤20MB。→ data: `{ "url":"/api/merchant/files/{32位uuid}.{ext}", "name":"原始文件名", "sizeBytes":123, "sizeKb":1, "ext":"png" }` |
| `GET /api/merchant/files/{url中的uuid.ext}` | **公开能力地址，无需令牌**（地址含不可猜测 UUID）。按类型内联返回：图片/视频内联预览，字幕 text/plain，其余附件下载。移动端用户展示商户商品图 / 视频直接用该 URL |

> 资质类文件（doc）链接不要对外传播。删除业务档案不会删除落盘文件。

## 四、接口清单（商户令牌，前缀 `/api/merchant`）

> 列表/详情的字段结构以 GET 实际返回为准（与网页端完全一致）；下表只列关键入参与返回要点。

| 模块 | 接口 | 方法 | 入参要点 | 返回 data 要点 |
|---|---|---|---|---|
| 认证 | `/login` `· /logout` `· /me` | POST·POST·GET | 见第二节 | 登录/登出/me 档案 |
| 工作台 | `/home` | GET | — | 首页汇总(商户名/欢迎语) |
| 经营概览 | `/overview` | GET | — | `kpis[4]`(曝光/消耗/GMV/订单) · `days[]/consume[]/gmv[]` 近7日 · `orderSummary{total,settled,pending,refunding}` |
| 投放计划 | `/plans` `/plans/slots` | GET·GET | — | 计划列表 / 可用广告位列表 |
| | `/plans` `/plans/{id}` | POST·PUT | `{name,adSlotId?,budget,dailyBudget?,...}` 演示字段以 GET 为准 | 计划对象 |
| | `/plans/{id}/pause` `/resume` `/plans/{id}` | PUT·PUT·DELETE | — | 操作结果 |
| 订单结算 | `/orders` | GET | — | 订单列表(最新在前,含状态/金额) |
| | `/orders/{id}/settle` | PUT | — | 结算该笔待结算订单 |
| 素材 | `/materials` `/materials/{id}` | GET·POST·PUT·DELETE | `{name,materialType?,sizeKb?,status?,fileName?,fileUrl?}`（上传文件先调 POST /files，fileUrl/fileName 随档） | 素材列表/对象（真实上传的带 fileUrl，可配预览） |
| | `/materials/ab` `/materials/ab` | GET·PUT | `{materialA,materialB,ratioB}` | A/B 分流配置 |
| 视频 | `/videos` `/videos/{id}` | GET·POST·PUT·DELETE | `{name,lang?,region?,status?,fileName?,fileUrl?}`（大小按落盘文件自动折算 GB） | 视频列表/对象 |
| | `/videos/stats` | GET | — | `{days,range[],totalPlays,avgFinishRate,dailyPlays,trend[{date,plays,finishRate}],regions[{region,plays,share}],videos[{videoId,name,status,region,plays,finishRate,watchSec}]}` |
| 字幕 | `/videos/{videoId}/subtitles` `/…/subtitles/{id}` | GET·POST·PUT·DELETE | `{lang(必填),fileName,fileUrl}`（文件先 POST /files kind=subtitle；同视频同语种唯一） | 字幕列表/对象（增删自动回写视频字幕语种） |
| 推广销售 | `/promotions` `/promotions/{id}` | GET·POST·PUT·DELETE | 渠道档案字段以 GET 为准 | 渠道列表/对象 |
| | `/sales` | GET | — | 销售报表 |
| 商品 | `/goods` `/goods/{id}` | GET·POST·PUT·DELETE | 商品字段以 GET 为准 | 商品列表/对象 |
| | `/goods/{id}/shelf` `· /unshelf` | PUT | — | 上架 / 下架 |
| 资金 | `/funds` | GET | — | `{balance,income,expense,flows[]}` 等 |
| | `/funds/recharge` `· /funds/withdraw` | POST | `{amount:数值}` | 充值 / 提现（提现走审核流） |
| 入驻 | `/onboarding` | GET | — | 当前商户最新申请(未提交为 null) |
| | `/onboarding/submit` | POST | `{merchantName,licenseNo,contact?,phone?,qualification}` | 申请单（资质文件先 POST /files kind=doc，qualification 记原始文件名） |
| | `/onboarding/contract/sign` | POST | — | 签署合作协议 |
| 资料 | `/profile` | GET·PUT | 联系人/电话/结算账户字段以 GET 为准 | 商户资料 |

## 五、用户端（C 端）预留约定

商户模块发布的以下内容是移动端用户侧要消费的数据，字段语义由商户模块维护：

| 数据 | 表 | 发布状态约定 | 用户侧取用方式 |
|---|---|---|---|
| 商品 | `merchant_goods` | `status`: 在售 / 已下架（`/goods/{id}/shelf·unshelf` 控制） | 用户侧只读接口由移动端同学另建（如 `/api/mall/goods`，放行用户角色），**不要**挂在 `/api/merchant` 下（商户令牌体系隔离） |
| 视频 | `merchant_video` | `status`: 已上架 / 已就绪 / 转码中；字幕见 `merchant_subtitle` | 同上；视频文件 URL 公开（见第三节），可直接用于播放器 |
| 推广渠道 | `merchant_channel` | 渠道链接 / 二维码由商户生成 | 用户点击渠道链接带来的成交按现有订单链路记账 |
| 广告素材 | `merchant_material`（含 fileUrl） | `status`: 使用中 / 测试中 / 已停用 | 图片/视频文件 URL 公开可取 |

C 端接口联调时如需新增字段（如商品简介、封面图），与商户模块负责人对齐字段名后由商户模块补列，避免两端字段漂移。

## 六、联调环境

- 后端启动环境变量：`ADMIN_DB_USER=shixun` / `ADMIN_DB_PASSWORD=123456` / `ADMIN_DDL_AUTO=update`（首个移动端联调如需建表用）
- 商户演示账号：`merchant / merchant123`；数据快照：`adminConsole/db/shixun.sql`
- 接口前缀可配置（`admin-console.api.base-path`，默认 `/api`），移动端不要写死 8080 之外的路径假设
